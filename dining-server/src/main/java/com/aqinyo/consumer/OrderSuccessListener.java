package com.aqinyo.consumer;

import com.aqinyo.dto.OrderDelayMessageDTO;
import com.aqinyo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.aqinyo.constant.RabbitMqConstant.*;

/*   监听器 --> 普通业务: 下单成功后(也可以是xx之后)其他业务的"异步"处理
*   (配置类只负责声明创建 / 监听器只负责监听  --> 实现声明监听分离 )
*/

@Slf4j
@Component
public class OrderSuccessListener {

    @Autowired
    private OrderService orderService;

   @RabbitListener(queues = ORDER_QUEUE_NAME)   //通过@RabbitListener引用即可监听指定的队列
    public void listenDirectQueue(OrderDelayMessageDTO messageDTO){
        log.info("消费者收到 order.queue 消息:" + ORDER_SUCCESS + ",订单id: 【" + messageDTO + "】");

        // 在这里调用 Service 实现真正的"下单成功"逻辑
       /* 补充: mq重要的不是发消息,而这个"邮局"的机制-->就是有生产者消费者监听的机制:让收到消息后可以去调用执行其他耗时的业务(这就是异步嘛) */
       // 比如下单后,还有其他业务就异步处理了:通知商家备货、数据统计、物流、商家端的一些数据更新 等等


    }

}
