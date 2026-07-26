package com.aqinyo.service;

import com.aqinyo.dto.*;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.OrderPaymentVO;
import com.aqinyo.vo.OrderStatisticsVO;
import com.aqinyo.vo.OrderSubmitVO;
import com.aqinyo.vo.OrderVO;

public interface OrderService {

    /*   用户、商家 共同接口方法   */
    // 订单详情
    OrderVO details(Long id);


    /*   用户端   */
    // 提交订单
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    // 订单支付
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    // 支付成功、修改订单状态
    void paySuccess(String outTradeNo);

    // 历史订单分页查询
    PageResult pageQueryByUser(int page, int pageSize, Integer status);

    // 再来一单(增加购物车菜品)
    void orderAgain(Long id);

    // 取消订单 (根据id)
    void cancelById(Long id);


    /*   商家端   */
    // 取消订单
    void cancel(OrdersCancelDTO ordersCancelDTO);

    // 订单条件分页查询
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    // 各订单数据统计
    OrderStatisticsVO statistics();

    // 接单 (确认订单)
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    // 拒单
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    // 派送订单
    void deliveryById(Long id);

    // 完成订单
    void complete(Long id);

}
