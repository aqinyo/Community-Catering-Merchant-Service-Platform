package com.aqinyo.constant;

/*   RabbitMQ相关常量类   */
public class RabbitMqConstant {

    public static final String ORDER_EXCHANGE_NAME = "order.direct";
    public static final String ORDER_QUEUE_NAME = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.routingKey";

    public static final String TTL_EXCHANGE_NAME = "ttl.direct";
    public static final String TTL_QUEUE_NAME = "ttl.queue";
    public static final String TTL_ROUTING_KEY = "ttl";

    public static final String DLX_EXCHANGE_NAME = "dlx.direct";
    public static final String DLX_QUEUE_NAME = "dlx.queue";
    public static final String DLX_ROUTING_KEY = "dlx";

    public static final String ORDER_SUCCESS = "你已成功下单！";
    public static final String ORDER_TIMEOUT = "订单超时未支付,已取消！";

}
