package com.aqinyo.consumer;

import com.aqinyo.dto.OrderDelayMessageDTO;
import com.aqinyo.service.OrderService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.aqinyo.constant.RabbitMqConstant.*;

/*   监听器 --> 核心业务: 订单超时-->自动取消 (延迟消息)
*   (配置类只负责声明创建 / 监听器只负责监听  --> 实现声明监听分离 )
*/

@Slf4j
@Component
public class OrderTimeOutListener {

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = DLX_QUEUE_NAME )   // 通过@RabbitListener引用,即可监听指定的队列 (一旦监听到里面来消息了就去消费,然后执行方法)
    public void listenDelayQueue(OrderDelayMessageDTO messageDTO){  // 这里入什么类型的参,取决于 rabbitTemplate.convertAndSend 那里发了什么类型的参
        log.info("消费者收到 dlx.queue 的延迟消息:{}, 订单id:{}", ORDER_TIMEOUT, messageDTO.getOrderId()); //【关于{}与参数对齐的原理】 @SLF4J 的 {} 是占位符,它与后续参数严格按照"出现顺序"一一对应  (代码中的换行、空格或缩进不影响对应关系)

        // 在这里调用 Service 实现真正的"取消订单"逻辑  (MQ本身不会取消订单,这里是告诉 Java 程序: 30分钟到了,该去取消了)
        orderService.cancelById(messageDTO.getOrderId());
    }

}
