package com.aqinyo.config;  // package声明当前类所在的包

import lombok.extern.slf4j.Slf4j;   // import导入当前类中需要依赖的外部类和接口
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/*  RabbitMQ配置类: 生产者消息可靠性回调 【本质是配置RabbitTemplate的行为】
 *    ConfirmCallback --> 确认消息是否成功到达 Exchange (返回ack/nack 给生产者)
 *    ReturnsCallback --> 确认消息是否成功路由到 Queue  (路由失败时退回消息 给生产者)
 *    ( yml负责开启开关 / 本类负责接收回调并处理(即通过log日志输出显示给我)  --> 实现开关与处理分离 )
 */

@Slf4j
@Component
public class RabbitMqCallbackConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*   将当前实例注册为RabbitTemplate的回调处理器  */
    @PostConstruct  // @PostConstruct: 确保该方法在依赖注入后(即RabbitTemplate被依赖注入后),立即执行该方法
    public void init() {
        rabbitTemplate.setConfirmCallback(this); //将当前类实现的 ConfirmCallback接口 注册给 RabbitTemplate: 用于处理消息到达 Exchange 的确认回调
        rabbitTemplate.setReturnsCallback(this); //将当前类实现的 ReturnsCallback接口 注册给 RabbitTemplate: 用于处理消息路由失败被退回的回调
    }


    /*   confirm机制: ConfirmCallback   */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("【confirm 机制】消息成功到达Exchange, correlationData: {}", correlationData);
        } else {
            log.error("【confirm 机制】消息未到达Exchange!!! correlationData: {}, 原因: {}", correlationData, cause);
            // 后续扩展: 落库记录 / 告警通知人工 ...
        }
    }


    /*   return机制: ReturnsCallback   */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        //【关于{}与参数对齐的原理】 @SLF4J 的 {} 是占位符,它与后续参数严格按照"出现顺序"一一对应  (代码中的换行、空格或缩进不影响对应关系)
        log.error("【returns 机制】消息路由Queue失败被退回!!! 交换机: {}, 路由键: {}, 原因: {}, 消息体: {}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getReplyText(),
                new String(returnedMessage.getMessage().getBody()));

        // 后续扩展: 落库记录 / 告警通知人工 ...
    }

}

