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

    @RabbitListener(queues = DLX_QUEUE_NAME )   ////通过@RabbitListener引用即可监听指定的队列 (一旦监听到里面来消息了就去消费,然后执行方法)
    public void listenDelayQueue(OrderDelayMessageDTO messageDTO){
        log.info("消费者收到 dlx.queue 的延迟消息:" + ORDER_TIMEOUT + ",订单id:【" + messageDTO + "】");

        // 在这里调用 Service 实现真正的"取消订单"逻辑  (MQ本身不会取消订单,这里是告诉 Java 程序: 30分钟到了,该去取消了)
        orderService.cancelById(messageDTO.getOrderId());
    }

}
