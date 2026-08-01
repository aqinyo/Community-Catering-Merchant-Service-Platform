package com.aqinyo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.aqinyo.constant.RabbitMqConstant.*;

/*  RabbitMQ配置类: 声明、创建、绑定交换机和队列 【本质是配置RabbitTemplate的行为】
*   (配置类只负责声明创建 / 监听器只负责监听  --> 实现声明监听分离 )
*/

@Configuration
public class RabbitMqTopologyConfig {

    /*   普通业务 - 下单成功   */
    // 声明交换机
    @Bean
    public DirectExchange orderExchange(){
        return new DirectExchange(ORDER_EXCHANGE_NAME);
    }
    // 声明队列
    @Bean
    public Queue orderQueue(){
        return QueueBuilder
                .durable(ORDER_QUEUE_NAME)
                .deadLetterExchange(DLX_FALLBACK_EXCHANGE_NAME)
                .deadLetterRoutingKey(DLX_FALLBACK_ROUTING_KEY)  // 隐式绑定 --> dlx.fallback.direct  (出现死信消息由 order.queue "转交"到 dlx.fallback.direct)
                .build();
    }
    // 绑定
    @Bean
    public Binding orderBinging(){
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }




    /*    TTL延迟业务  (订单超时取消)    */
    // 声明ttl交换机
    @Bean
    public DirectExchange ttlDirectExchange(){
        return new DirectExchange(TTL_EXCHANGE_NAME);
    }
    // 声明ttl队列
    @Bean
    public org.springframework.amqp.core.Queue ttlQueue(){    /*  同时 通过"设定死信路由key" 来 "隐式绑定" 下面一层的死信交换机   */
        return QueueBuilder
                .durable(TTL_QUEUE_NAME)
                .ttl(30*1000)                            // ttl属性负责"延迟": 会让消息进入queue中被倒计时,实现了延迟功能
                .deadLetterExchange(DLX_EXCHANGE_NAME)   // deadLetter死信属性负责"转交": 是给 "延迟队列ttl.queue" 加的死信属性噢~ 而不是加给 dlx.direct / dlx.queue
                .deadLetterRoutingKey(DLX_ROUTING_KEY)   // 隐式绑定 --> dlx.direct  (转交到超时取消的死信交换机里)      (小插曲: 一开始2.1.8版本是不支持这种语法,我升级了SpringBoot版为2.3.12后才消掉爆红)
                .lazy()                                  // 声明为惰性队列   (写在延迟队列上的,因为有TTL的延迟控制,所以可能会有大量消息堆积的情况)
                .build();
    }
    // 绑定
    @Bean
    public Binding ttlBinding(){    /* 这个才是显示绑定 */
        return BindingBuilder.bind(ttlQueue()).to(ttlDirectExchange()).with(TTL_ROUTING_KEY);
    }




    /*   死信业务 - 超时取消 (搭配TTL)   */
    // 声明超时死信交换机
    @Bean
    public DirectExchange dlxDirectExchange(){
        return new DirectExchange(DLX_EXCHANGE_NAME);//补充:死信交换机不是类型,而是把普通交换机--用作--> 死信交换机(下面的队列同理)
    }
    // 声明超时死信队列
    @Bean
    public org.springframework.amqp.core.Queue dlxQueue(){
        return QueueBuilder
                .durable(DLX_QUEUE_NAME)
                .deadLetterExchange(DLX_FALLBACK_EXCHANGE_NAME)
                .deadLetterRoutingKey(DLX_FALLBACK_ROUTING_KEY)    // 隐式绑定 --> dlx.fallback.direct  (转交到异常消息兜底的死信交换机里)
                .build();   // 在"订单超时取消"中: dlx.queue 只是普通队列,它只负责接收和存储过期的消息(与ttl.queue打配合的),真正执行"转交"死信消息的是 ttl.queue      【第一级死信】
    }                       // 在"异常消息兜底"中: dlx.queue 就得加上死信属性变成死信队列了的！因为当消费者消费失败重试仍失败后,真正执行"转交"死信消息的就是 dlx.queue    【第二级死信】
    // 绑定
    @Bean
    public Binding dlxBinging(){
        return BindingBuilder.bind(dlxQueue()).to(dlxDirectExchange()).with(DLX_ROUTING_KEY);
    }




    /*   死信业务 - 全局异常消息兜底   */
    // 声明死信兜底交换机
    @Bean
    public DirectExchange dlxFallbackDirectExchange(){
        return new DirectExchange(DLX_FALLBACK_EXCHANGE_NAME);
    }
    // 声明死信兜底队列
    @Bean
    public org.springframework.amqp.core.Queue dlxFallbackQueue(){
        return QueueBuilder
                .durable(DLX_FALLBACK_QUEUE_NAME)
                .build();   // dlx.fallback.queue 本身就是一个专门用于接收和存储异常消息的"普通队列" (是整个消息链路的"终点站/垃圾桶",所有死信消息最终都堆在这里,不需要加死信属性去"转发"了)
    }
    // 绑定
    @Bean
    public Binding dlxFallbackBinging(){
        return BindingBuilder.bind(dlxFallbackQueue()).to(dlxFallbackDirectExchange()).with(DLX_FALLBACK_ROUTING_KEY);
    }

}
