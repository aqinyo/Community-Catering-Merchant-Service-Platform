package com.aqinyo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/*  Redis配置类: (手动式缓存)自定义RedisTemplate的序列化配置 + (注解式缓存)自定义CacheManager的序列化配置   【本质是配置Redis的行为】 (可复用)
*     自定义目的: 为了支持存储复杂的 Java 对象、Map、List 等复杂类型, 自动完成对象和 JSON 的互转
*     本质: 两者都是在配置 "Java对象 <--> Redis存储" 的序列化规则
*     ( 两者共享同一个ObjectMapper, 保证Redis中所有数据统一JSON可读 )
 */

@Configuration
@Slf4j
public class RedisJsonConfig {

    /*   公共部分: 共享的ObjectMapper  (手动式 + 注解式共用, 保证序列化规则一致)   */
    private static ObjectMapper getObjectMapper() {
        // 1. 手动构造ObjectMapper，修复Object类型的序列化问题
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 注册 Java 8 时间模块

        // 核心配置: 所有非final类型（包括Object、集合、自定义类）都写入类型id
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);   // 兼容配置: 反序列化时忽略未知字段,避免后续字段增减报错
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   // 日期序列化优化: 不转时间戳，用标准格式
        return objectMapper;
    }



    /*   手动式缓存: 自定义RedisTemplate的序列化配置   */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建自定义(手动式缓存)RedisTemplate模板对象..."); // 即: 后面依赖注入的RedisTemplate都是我自定义的RedisTemplate

        // 创建 RedisTemplate 对象
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 用自定义且共享的ObjectMapper 创建 Value 使用的JSON序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(getObjectMapper());

        /*   标准配置通常会同步设置四个序列化器,如下所示:   */
        // 设置 Key 的序列化         ( Key 永远都是String序列化 --> 让Key变得可读 )
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());   // Hash类型比较特殊(嵌套的key-value),要针对设置一下,其他的已经被上面一行的设置了
        // 设置 Value 的序列化       ( Value 用Json序列化处理 --> 让value变得可读 )
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);     // Hash类型比较特殊(嵌套的key-value),要针对设置一下,其他的已经被上面一行的设置了

        // 必须调用: 让所有配置生效
        redisTemplate.afterPropertiesSet();

        log.info("自定义(手动式缓存)RedisTemplate序列化配置 加载完成......");
        return redisTemplate;
    }



    /*   注解式缓存: 自定义CacheManager的序列化配置   */
    //注解式缓存是不依赖注入RedisTemplate的噢！依赖的是Spring底层提供的CacheManager缓存管理器 (这里是让@Cacheable/@CacheEvict...底层也用自定义的JSON序列化)
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建自定义(注解式缓存)CacheManager缓存管理器对象...");

        // 用自定义且共享的ObjectMapper 创建 Value 使用的JSON序列化工具
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(getObjectMapper());

        // 配置 Redis 注解式缓存的默认行为
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 设置 缓存默认过期时间为1小时
                // 设置 Key 的序列化         ( Key 永远都是String序列化 --> 让Key变得可读 )
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                // 设置 Value 的序列化       ( Value 用Json序列化处理 --> 让value变得可读 )
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues(); // 禁止 缓存null,避免缓存穿透

        log.info("自定义(注解式缓存)CacheManager序列化配置 加载完成......");

        // 构建并返回 RedisCacheManager
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }

}

