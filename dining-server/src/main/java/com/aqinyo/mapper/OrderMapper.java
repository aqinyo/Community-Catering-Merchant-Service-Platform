package com.aqinyo.mapper;

import com.github.pagehelper.Page;
import com.aqinyo.dto.GoodsSalesDTO;
import com.aqinyo.dto.OrdersPageQueryDTO;
import com.aqinyo.entity.Orders;
import com.aqinyo.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    // 插入订单
    void insert(Orders orders);

    // 根据订单号查询订单
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    // 修改订单信息
    void update(Orders orders);

    // 分页查询
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    // 根据状态统计订单数量
    @Select("select count(id) from orders where status = #{status}")
    int getCountByStatus(Integer status);

    // 根据订单状态和下单时间查询订单
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getStatusAndOrderTImeLT(Integer status, LocalDateTime orderTime);

}
