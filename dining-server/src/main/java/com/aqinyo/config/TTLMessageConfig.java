package com.aqinyo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.aqinyo.constant.RabbitMqConstant.*;

/*  配置类: 声明和创建交换机和队列
*   (配置类只负责声明创建 / 监听器只负责监听  --> 实现声明监听分离 )
*/

@Configuration
public class TTLMessageConfig {

    /*   普通业务  (下单成功)   */
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
                .build();
    }
    // 绑定
    @Bean
    public Binding orderBinging(){
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }



    /*   死信业务 (搭配TTL)   */
    // 声明死信交换机
    @Bean
    public DirectExchange dlxDirectExchange(){
        return new DirectExchange(DLX_EXCHANGE_NAME);//补充:死信交换机不是类型,而是把普通交换机--用作--> 死信交换机(下面的队列同理)
    }
    // 声明死信队列
    @Bean
    public org.springframework.amqp.core.Queue dlxQueue(){
        return QueueBuilder
                .durable(DLX_QUEUE_NAME)
                .build();
    }
    // 绑定
    @Bean
    public Binding dlxBinging(){
        return BindingBuilder.bind(dlxQueue()).to(dlxDirectExchange()).with(DLX_ROUTING_KEY);
    }



    /*    TTL延迟业务  (订单超时取消)    */
    // 声明ttl交换机
    @Bean
    public DirectExchange ttlDirectExchange(){
        return new DirectExchange(TTL_EXCHANGE_NAME);
    }
    // 声明ttl队列
    @Bean
    public org.springframework.amqp.core.Queue ttlQueue(){    /*  同时 "隐式绑定" 下面一层的 --> 死信交换机 + 设定key  */
        return QueueBuilder
                .durable(TTL_QUEUE_NAME)
                .ttl(30*1000)   //指消息进入该queue的指定时间(即延迟时间)内没人消费,就变成死信-->进入dlx.queue死信队列(这就是延迟的流程)
                .deadLetterExchange(DLX_EXCHANGE_NAME)   //隐式绑定-->死信交换机(并加上key)  (并且是给延迟队列ttl.queue加的死信属性噢~ 而不是加在dlx.queue)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)   //小插曲: 一开始2.1.8版本是不支持这种语法,我升级了SpringBoot版为2.3.12后才消掉爆红
                .lazy()     // 声明为惰性队列   (写在延迟队列上的,因为有延迟,所以可能有大量堆积的消息)
                .build();
    }
    // 绑定
    @Bean
    public Binding ttlBinding(){    /* 这个才是显示绑定 */
        return BindingBuilder.bind(ttlQueue()).to(ttlDirectExchange()).with(TTL_ROUTING_KEY);
    }

}
