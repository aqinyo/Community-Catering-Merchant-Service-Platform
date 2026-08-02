package com.aqinyo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.context.BaseContext;
import com.aqinyo.dto.*;
import com.aqinyo.entity.*;
import com.aqinyo.exception.AddressBookBusinessException;
import com.aqinyo.exception.OrderBusinessException;
import com.aqinyo.exception.ShoppingCartBusinessException;
import com.aqinyo.mapper.*;
import com.aqinyo.result.PageResult;
import com.aqinyo.service.OrderService;
import com.aqinyo.vo.OrderPaymentVO;
import com.aqinyo.vo.OrderStatisticsVO;
import com.aqinyo.vo.OrderSubmitVO;
import com.aqinyo.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.aqinyo.constant.RabbitMqConstant.*;

/*   C端  订单管理   */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;  // 订单明细表
    @Autowired
    private AddressBookMapper addressBookMapper; //处理 地址 可能为空的业务异常要使用到
    @Autowired
    private ShoppingCartMapper shoppingCartMapper; //处理 购物车 可能为空的业务异常要使用到
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;  // 引入做: 订单超时自动取消


    /*   用户端 - 提交订单   */
    @Override
    @Transactional  //开启事务注解-->为了保证数据一致性  (若订单表的数据插入成功,而与其相关联的订单明细表插入失败,则数据不一致了-->因此设涉及到这类情况的都需要开启一个事务注解)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        /* 处理各种业务异常 (2个) */
            // 1、地址是否为空？
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());//从前端发来的DTO类中get地址的id去判断是否为空
        if(addressBook == null){
                //确认为空,抛出异常提示
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);//自定义的常量提示
        }
            // 2、购物车是否为空？
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .build(); /*  builder是做"单个类"对象创建的,一次性创建对象和属性赋值一条链式里优雅完成(与属性拷贝+手动赋值不同:这是做"两个不同类"转换的)  */
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()){
                //确认为空,抛出异常提示
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向 "订单表" 插入 "一条" 数据   (与下面的明细表不同,订单一般生成一次,然后订单里面有多个明细菜品,所以下面订单明细表是批量插入)
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);  // 依旧是 对象属性拷贝 + 手动赋值 (下面的)
        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(BaseContext.getCurrentId());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());
        //调用mapper,插入封装好的实体类(这里插入成功后才能去调用生产者去发消息,因为要基于插进去数据的订单id)
        orderMapper.insert(orders);

        //向 "订单明细表" 插入 "多条" 数据    (这个操作要用到上面的订单表的id-->所以开启了在xml文件orderMapper.insert的SQL中的useGeneratedKeys属性)
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);// 和上面插入一条数据一样:也是要转换 (只不过是在循环里重复多次)
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);


        /*   调用 RabbitMQ 生产者发消息 --> 下单成功(必要前提) + 订单超时自动取消   */
            //在订单数据插入数据库之后-->才可以拿到数据库中订单表的id-->然后基于这个id去发消息
        Long newOrderId = orders.getId();
        log.info("订单落库成功,生成的订单ID为: {}", newOrderId);

        OrderDelayMessageDTO orderSuccess = new OrderDelayMessageDTO();
        orderSuccess.setOrderId(newOrderId);

        try {   //这里捕获异常 --是优化--> 防止发消息失败而影响了主业务  (还好加了:一开始还真异常了-->RabbitMQ消息收发的序列化异常导致的)
            /* 发送 "下单成功" 的消息 */
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE_NAME,ORDER_ROUTING_KEY,orderSuccess);//"交换机名称+路由键+发送的消息" (发的是DTO类参数对象噢,里面是落库后查出来的订单id)
            /* 发送 "30分钟延迟超时" 的消息 */
            rabbitTemplate.convertAndSend(TTL_EXCHANGE_NAME,TTL_ROUTING_KEY,orderSuccess);
        }catch (Exception e){
            log.error("发送MQ消息失败,但已成功下单,后续需人工排查,订单ID: {}",newOrderId, e);
        }
        /* 总结: 就是原本这里应该有一大坨业务要去做的,而且还很可能是分别去进行增删改查,等都执行完成-->再返回给controller层然后给前端,这就是说的:同步+高耦合+响应慢)  TODO:自己的MQ总结精髓
                这时候加入了MQ就是把原本的一大坨业务代码抽走,我这里只发消息给MQ就直接返回给controller然后走前端(即异步解耦:原来一大坨的就异步处理了)然后等监听器收到MQ发来的消息再去调用执行,不影响前面的快速返回了！ */


        //清空当前用户的购物车数据
        shoppingCartMapper.clean(BaseContext.getCurrentId());

        //封装VO类返回数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .build();
        return orderSubmitVO;
    }


    /*   用户端 - 订单支付  (流程非常规定,理解即可,用到时完全可以复制使用) */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口,生成预支付交易单 (现在暂时用不上先注释)
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "商城外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }


    /*   用户端 - 支付成功、修改订单状态   */
    /**
     * 该方法用于处理支付成功后的回调逻辑。
     * 当用户完成支付后，根据商户订单号（outTradeNo）查询订单，
     * 并将订单状态更新为“待接单”，支付状态更新为“已支付”（PAID），同时记录结账时间。
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 判断订单是否存在，若不存在则抛出业务异常
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /*   用户端 - 历史订单分页查询   */
    public PageResult pageQueryByUser(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /*   用户端 - 再来一单 (增加购物车菜品)   */
    @Override
    public void orderAgain(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /*   用户端、商家端公共 - 订单详情   */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /*   用户端 - 取消订单   */
    @Override
    public void cancelById(Long id) {
        Orders orderDB = orderMapper.getById(id);
        //判断订单是否存在
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断订单是否已有骑手接单
        if(orderDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);
        //判断订单是否需要退款
        if(orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
//            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


    /*   商家端 - 订单条件分页查询   */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 条件分页查询订单列表
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 额外查询并封装订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        // 封装分页结果返回
        return new PageResult(page.getTotal(), orderVOList);
    }


    /*   商家端 - 各订单数据统计   */
    @Override
    public OrderStatisticsVO statistics() {
        int toBeConfirmed = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        int confirmed = orderMapper.getCountByStatus(Orders.CONFIRMED);
        int deliveryInProgress = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);
        return new OrderStatisticsVO(toBeConfirmed, confirmed, deliveryInProgress);
    }


    /*   商家端 - 接单 (确认订单)   */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                        .id(ordersConfirmDTO.getId())
                        .status(Orders.CONFIRMED)
                        .build();
        orderMapper.update(orders);
    }


    /*   商家端 - 拒单   */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        //判断订单是否存在，如果订单不是2：待接单则不能拒单
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if(ordersDB.getPayStatus().equals(Orders.PAID)){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款");
        }
        Orders orders = Orders.builder()
                .status(Orders.CANCELLED)
                .id(ordersDB.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }


    /*   商家端 - 取消订单   */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .build();

        if(ordersDB.getPayStatus().equals(Orders.PAID)){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款：{}", ordersDB.getAmount());
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }


    /*   商家端 - 派送订单   */
    @Override
    public void deliveryById(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }


    /*   商家端 - 完成订单   */
    @Override
    public void complete(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(orders);
    }

    /**
        内部辅助方法: 将分页的订单实体列表转换为包含菜品明细的 OrderVO 列表。
        (注: 此方法为私有辅助方法,仅供本类内部的 "订单分页查询接口" 调用,用于组装响应数据)
    */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 1. 将订单实体的共同字段复制到 OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                
                // 2. 调用内部辅助方法获取菜品明细字符串，并封装到 orderVO 中
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
        内部辅助方法: 根据订单ID查询订单明细,并将菜品名称与数量拼接为指定格式的字符串。
        (注: 此方法为私有辅助方法,仅供本类内部的 "getOrderVOList"方法 调用,用于组装订单的菜品信息字符串)
    */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

}
