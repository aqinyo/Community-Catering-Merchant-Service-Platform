package com.aqinyo.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 *   配置JSON消息转换器,替换默认的SimpleMessageConverter
 *   生产者-->发消息:(DTO类)自动转JSON,  消费者-->收消息:JSON自动转回Java对象
 *              序列化                         反序列化
 */

@Configuration
public class RabbitJsonConfig {

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
