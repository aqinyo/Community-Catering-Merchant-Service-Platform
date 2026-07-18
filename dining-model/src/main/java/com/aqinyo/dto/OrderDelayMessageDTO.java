package com.aqinyo.dto;

import lombok.Data;

/*   消息队列 DTO类   */

@Data
public class OrderDelayMessageDTO {

    private Long orderId; // 核心: 用于识别是 "数据库订单表" 中的哪个订单

}
