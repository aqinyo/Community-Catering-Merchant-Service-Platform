package com.aqinyo.service;

import com.aqinyo.dto.*;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.OrderPaymentVO;
import com.aqinyo.vo.OrderStatisticsVO;
import com.aqinyo.vo.OrderSubmitVO;
import com.aqinyo.vo.OrderVO;

public interface OrderService {

    // 用户下单
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    // 订单支付
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    // 支付成功、修改订单状态
    void paySuccess(String outTradeNo);

    PageResult pageQueryByUser(int page, int pageSize, Integer status);

    // 增加购物车菜品
    void orderAgain(Long id);

    OrderVO details(Long id);

    // 取消订单 (根据id)
    void cancelById(Long id);

    // 订单分页查询
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    // 拒单
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void cancel(OrdersCancelDTO ordersCancelDTO);

    // 派送订单
    void deliveryById(Long id);

    // 完成订单
    void complete(Long id);

}
