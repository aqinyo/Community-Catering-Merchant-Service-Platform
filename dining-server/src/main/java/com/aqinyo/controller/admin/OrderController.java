package com.aqinyo.controller.admin;

import com.aqinyo.dto.OrdersCancelDTO;
import com.aqinyo.dto.OrdersConfirmDTO;
import com.aqinyo.dto.OrdersPageQueryDTO;
import com.aqinyo.dto.OrdersRejectionDTO;
import com.aqinyo.result.PageResult;
import com.aqinyo.result.Result;
import com.aqinyo.service.OrderService;
import com.aqinyo.vo.OrderStatisticsVO;
import com.aqinyo.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/*   订单管理  controller层   */

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "admin端-订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /*   订单分页查询   */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单分页查询")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /*   各订单数据统计   */
    @GetMapping("/statistics")
    @ApiOperation("各订单数量统计")
    public Result<OrderStatisticsVO> statistics(){
        log.info("各个订单数量统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /*   订单详情   */
    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> detail(@PathVariable Long id){
        log.info("订单详情：{}", id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /*   接单   */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单：{}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /*   拒单   */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        log.info("拒单：{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /*   取消订单   */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("取消订单：{}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /*   派送订单   */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id){
        log.info("派送订单：{}", id);
        orderService.deliveryById(id);
        return Result.success();
    }

    /*   完成订单   */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id){
        log.info("完成订单：{}", id);
        orderService.complete(id);
        return Result.success();
    }

}
