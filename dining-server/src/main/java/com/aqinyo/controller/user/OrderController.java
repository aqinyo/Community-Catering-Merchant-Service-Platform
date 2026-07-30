package com.aqinyo.controller.user;

import com.aqinyo.dto.OrdersPageQueryDTO;
import com.aqinyo.dto.OrdersPaymentDTO;
import com.aqinyo.dto.OrdersSubmitDTO;
import com.aqinyo.result.PageResult;
import com.aqinyo.result.Result;
import com.aqinyo.service.OrderService;
import com.aqinyo.vo.OrderPaymentVO;
import com.aqinyo.vo.OrderSubmitVO;
import com.aqinyo.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")  //起别名,防止与管理端的订单服务重复
@RequestMapping("/user/order")
@Api(tags = "user端-订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /*   提交订单   */
    @PostMapping("/submit")
    @ApiOperation("提交订单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户提交订单:{}", ordersSubmitDTO);
        /* 这里调用service方法时,我加入RabbitMQ进去(生产者发消息) */
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /*   订单支付   */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }

    /*   历史订单查询   */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQueryByUser(page, pageSize, status);
        return Result.success(pageResult);
    }

    /*   再来一单   */
    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result<String> orderAgain(@PathVariable Long id){
        log.info("再来一单：{}", id);
        orderService.orderAgain(id);
        return Result.success();
    }

    /*   订单详情   */
    @GetMapping("orderDetail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("订单详情：{}", id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /*   取消订单   */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result<String> cancelOrder(@PathVariable Long id){
        log.info("取消订单：{}", id);
        orderService.cancelById(id);
        return Result.success();
    }

}
