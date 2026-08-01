package com.aqinyo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;



/*  Redis配置类: 自定义Redis的序列化模板   【本质是配置Redis的行为】
*    为了支持存储复杂的 Java 对象、Map、List 等复杂类型, 自动完成对象和 JSON 的互转
*    ( 默认的StringRedisTemplate模板则只能传String类型 )   (下面写好后,之后都可以复用,注入自定义的redisTemplate模板了)
*/


@Configuration
@Slf4j
public class RedisJsonConfig {
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建自定义的redis模板对象.....自定义的 Redis序列化配置 加载成功......");

        // 创建 RedisTemplate 对象  (往下有序号的注释的均是新加的-->修改报错的调整)
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate();

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 1. 手动构造ObjectMapper，修复Object类型的序列化问题
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 注册 Java 8 时间模块
        // 1.1 核心配置：所有非final类型（包括Object、集合、自定义类）都写入类型id
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 1.2 兼容配置：反序列化时忽略未知字段，避免后续字段增减报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 1.3 日期序列化优化：不转时间戳，用标准格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. 用自定义的ObjectMapper创建JSON序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);


        // 设置 redis 连接的工厂对象
        //redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建 Value 使用的JSON序列化工具
        GenericJackson2JsonRedisSerializer JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);


/*   标准配置通常会同步设置四个序列化器,如下所示:   */

        // 设置 Key 的序列化         ( 永远都是String序列化 --> 让Key变得可读)
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());//hash类型比较特殊(嵌套的key-value),要针对设置一下,其他的已经被上面一行的设置了

        // 设置value的序列化        ( value 用json处理 --> 让value变得可读 )
        redisTemplate.setValueSerializer(JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(JsonRedisSerializer);//hash类型比较特殊(嵌套的key-value),要针对设置一下,其他的已经被上面一行的设置了

        // 3.必须调用: 让所有配置生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;

    }
}

