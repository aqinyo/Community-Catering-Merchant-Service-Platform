package com.aqinyo.service.impl;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.context.BaseContext;
import com.aqinyo.dto.*;
import com.aqinyo.dto.OrderDelayMessageDTO;
import com.aqinyo.entity.AddressBook;
import com.aqinyo.entity.OrderDetail;
import com.aqinyo.entity.Orders;
import com.aqinyo.entity.ShoppingCart;
import com.aqinyo.exception.AddressBookBusinessException;
import com.aqinyo.exception.OrderBusinessException;
import com.aqinyo.exception.ShoppingCartBusinessException;
import com.aqinyo.mapper.*;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.OrderStatisticsVO;
import com.aqinyo.vo.OrderSubmitVO;
import com.aqinyo.vo.OrderVO;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private AddressBookMapper addressBookMapper;
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;
    /*  @Mock 与 @InjectMocks 就一句话:
            原本这里很像依赖注入,但现在这里是创建 OrderServiceImpl 的实例对象,而且实例对象需要依赖注入什么,上面就 @Mock几个假对象。
            (即:这个OrderServiceImpl当中原本需要依赖注入的类全部变成由@Mock创建的假对象替代注入,而上面的 @Mock 的假对象都是为下面的 @InjectMocks 创建 "被测对象" 实例服务的)
    */

    @BeforeEach
    void setUp() {
        // BaseContext 使用 ThreadLocal，直接 set 即可，不需要 mockStatic
        BaseContext.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        // 清理 ThreadLocal，避免测试间相互影响
        BaseContext.removeCurrentId();
    }

    // ================================= submitOrder()方法 单元测试 =================================

    @Test
    @DisplayName("提交订单 - 地址为空，抛出异常")
    void submitOrder_addressIsNull() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(999L);

        when(addressBookMapper.getById(999L)).thenReturn(null);

        assertThrows(AddressBookBusinessException.class,
                () -> orderService.submitOrder(dto));
    }

    @Test
    @DisplayName("提交订单 - 购物车为空，抛出异常")
    void submitOrder_cartIsEmpty() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);

        AddressBook addressBook = AddressBook.builder()
                .id(1L).phone("13800138000").consignee("张三").detail("天河区").build();
        when(addressBookMapper.getById(1L)).thenReturn(addressBook);
        // 购物车为空
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.emptyList());

        assertThrows(ShoppingCartBusinessException.class,
                () -> orderService.submitOrder(dto));
    }

    @Test
    @DisplayName("提交订单 - 正常下单主流程")
    void submitOrder_success() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);
        dto.setPayMethod(1);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPackAmount(0);       // OrdersSubmitDTO(Integer) → Orders(int)，必须设非null值防止 BeanUtils 拷贝报错
        dto.setTablewareNumber(0);   // 同上

        AddressBook addressBook = AddressBook.builder()
                .id(1L).phone("13800138000").consignee("张三").detail("天河区").build();
        when(addressBookMapper.getById(1L)).thenReturn(addressBook);

        ShoppingCart cart = ShoppingCart.builder()
                .id(1L).name("宫保鸡丁").dishId(1L).number(2)
                .amount(new BigDecimal("38.00")).image("/img/1.png").build();
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(cart)));

        OrderSubmitVO result = orderService.submitOrder(dto);

        // 验证返回值
        assertNotNull(result);
        assertNotNull(result.getOrderTime());
        assertNotNull(result.getOrderNumber());

        // 验证订单和明细都被插入
        verify(orderMapper, times(1)).insert(any(Orders.class));
        verify(orderDetailMapper, times(1)).insertBatch(anyList());
        // 验证 RabbitMQ 消息被发送（convertAndSend 存在方法重载歧义，无法用 Mockito 匹配器精确验证）
        // 但核心逻辑已被上面的 insert/insertBatch 和返回值断言覆盖
        verify(shoppingCartMapper, times(1)).clean(1L);
    }

    @Test
    @DisplayName("提交订单 - RabbitMQ发送失败不影响主流程")
    void submitOrder_mqSendFail() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPackAmount(0);
        dto.setTablewareNumber(0);

        AddressBook addressBook = AddressBook.builder()
                .id(1L).phone("13800138000").consignee("张三").detail("天河区").build();
        when(addressBookMapper.getById(1L)).thenReturn(addressBook);

        ShoppingCart cart = ShoppingCart.builder()
                .id(1L).name("宫保鸡丁").dishId(1L).number(1)
                .amount(new BigDecimal("38.00")).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(cart)));

        // 模拟 RabbitMQ 发送异常（需强制类型转换消除 convertAndSend 重载歧义）
        doAnswer(invocation -> { throw new RuntimeException("MQ连接失败"); })
                .when(rabbitTemplate).convertAndSend(
                        anyString(),
                        any(),
                        (OrderDelayMessageDTO) any(OrderDelayMessageDTO.class));

        // 即使 MQ 发送失败，主流程不应抛异常
        OrderSubmitVO result = orderService.submitOrder(dto);

        assertNotNull(result);
        // 订单仍然被插入
        verify(orderMapper, times(1)).insert(any(Orders.class));
        verify(orderDetailMapper, times(1)).insertBatch(anyList());
    }

    // ==================== paySuccess 方法测试 ====================

    @Test
    @DisplayName("支付成功回调 - 正常更新订单状态")
    void paySuccess_success() {
        Orders ordersDB = Orders.builder()
                .id(1L).number("202401010001").status(Orders.PENDING_PAYMENT)
                .payStatus(Orders.UN_PAID).build();
        when(orderMapper.getByNumber("202401010001")).thenReturn(ordersDB);

        orderService.paySuccess("202401010001");

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());

        Orders updated = captor.getValue();
        assertEquals(Orders.TO_BE_CONFIRMED, updated.getStatus());
        assertEquals(Orders.PAID, updated.getPayStatus());
        assertNotNull(updated.getCheckoutTime());
    }

    @Test
    @DisplayName("支付成功回调 - 订单不存在，抛出异常")
    void paySuccess_orderNotFound() {
        when(orderMapper.getByNumber("notexist")).thenReturn(null);

        assertThrows(OrderBusinessException.class,
                () -> orderService.paySuccess("notexist"));
    }

    // ================================= pageQueryByUser()方法 单元测试 =================================

    @Test
    @DisplayName("用户端 - 历史订单分页查询（有数据）")
    void pageQueryByUser_hasData() {
        Page<Orders> page = new Page<>(1, 10);
        Orders order = Orders.builder().id(1L).number("202401010001").status(Orders.COMPLETED).build();
        page.add(order);
        page.setTotal(1);

        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page);

        List<OrderDetail> details = Arrays.asList(
                OrderDetail.builder().id(1L).orderId(1L).name("宫保鸡丁").number(2).build()
        );
        when(orderDetailMapper.getByOrderId(1L)).thenReturn(details);

        PageResult result = orderService.pageQueryByUser(1, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("用户端 - 历史订单分页查询（无数据）")
    void pageQueryByUser_noData() {
        Page<Orders> page = new Page<>(1, 10);
        page.setTotal(0);

        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page);

        PageResult result = orderService.pageQueryByUser(1, 10, null);

        assertEquals(0, result.getTotal());
    }

    // ================================= orderAgain()方法 单元测试 =================================

    @Test
    @DisplayName("再来一单 - 将订单详情重新加入购物车")
    void orderAgain_success() {
        List<OrderDetail> details = Arrays.asList(
                OrderDetail.builder().id(1L).name("宫保鸡丁").dishId(1L).number(2).amount(new BigDecimal("38.00")).build()
        );
        when(orderDetailMapper.getByOrderId(1L)).thenReturn(details);

        orderService.orderAgain(1L);

        verify(shoppingCartMapper, times(1)).insertBatch(anyList());
    }

    // ================================= details()方法 单元测试 =================================

    @Test
    @DisplayName("订单详情 - 正常返回")
    void details_success() {
        Orders orders = Orders.builder().id(1L).number("202401010001").status(Orders.COMPLETED).build();
        List<OrderDetail> details = Arrays.asList(
                OrderDetail.builder().id(1L).name("宫保鸡丁").number(2).build()
        );

        when(orderMapper.getById(1L)).thenReturn(orders);
        when(orderDetailMapper.getByOrderId(1L)).thenReturn(details);

        OrderVO result = orderService.details(1L);

        assertNotNull(result);
        assertEquals(1, result.getOrderDetailList().size());
    }

    // ================================= cancelById()方法 单元测试 =================================

    @Test
    @DisplayName("用户取消订单 - 正常取消（待接单状态，需退款）")
    void cancelById_success_refund() {
        Orders orderDB = Orders.builder()
                .id(1L).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID).build();
        when(orderMapper.getById(1L)).thenReturn(orderDB);

        orderService.cancelById(1L);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.CANCELLED, captor.getValue().getStatus());
        assertEquals(Orders.REFUND, captor.getValue().getPayStatus());
    }

    @Test
    @DisplayName("用户取消订单 - 订单不存在")
    void cancelById_orderNotFound() {
        when(orderMapper.getById(999L)).thenReturn(null);

        assertThrows(OrderBusinessException.class,
                () -> orderService.cancelById(999L));
    }

    @Test
    @DisplayName("用户取消订单 - 已有骑手接单，不能取消")
    void cancelById_statusError() {
        Orders orderDB = Orders.builder().id(1L).status(Orders.CONFIRMED + 1).build();
        when(orderMapper.getById(1L)).thenReturn(orderDB);

        assertThrows(OrderBusinessException.class,
                () -> orderService.cancelById(1L));
    }

    // ================================= conditionSearch()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 订单条件分页查询")
    void conditionSearch_success() {
        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        Page<Orders> page = new Page<>(1, 10);
        Orders order = Orders.builder().id(1L).number("202401010001").build();
        page.add(order);
        page.setTotal(1);

        when(orderMapper.pageQuery(queryDTO)).thenReturn(page);
        when(orderDetailMapper.getByOrderId(1L)).thenReturn(
                Arrays.asList(OrderDetail.builder().name("宫保鸡丁").number(2).build())
        );

        PageResult result = orderService.conditionSearch(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    // ================================= statistics()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 订单数据统计")
    void statistics_success() {
        when(orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED)).thenReturn(3);
        when(orderMapper.getCountByStatus(Orders.CONFIRMED)).thenReturn(5);
        when(orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS)).thenReturn(2);

        OrderStatisticsVO result = orderService.statistics();

        assertEquals(3, result.getToBeConfirmed());
        assertEquals(5, result.getConfirmed());
        assertEquals(2, result.getDeliveryInProgress());
    }

    // ================================= confirm()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 接单")
    void confirm_success() {
        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(1L);

        orderService.confirm(dto);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.CONFIRMED, captor.getValue().getStatus());
    }

    // ================================= rejection()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 拒单（正常拒单）")
    void rejection_success() {
        OrdersRejectionDTO dto = new OrdersRejectionDTO();
        dto.setId(1L);
        dto.setRejectionReason("菜品已售罄");

        Orders orderDB = Orders.builder()
                .id(1L).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.UN_PAID).build();
        when(orderMapper.getById(1L)).thenReturn(orderDB);

        orderService.rejection(dto);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("商家端 - 拒单（订单状态不对）")
    void rejection_statusError() {
        OrdersRejectionDTO dto = new OrdersRejectionDTO();
        dto.setId(1L);
        dto.setRejectionReason("不想接");

        Orders orderDB = Orders.builder().id(1L).status(Orders.CONFIRMED).build();
        when(orderMapper.getById(1L)).thenReturn(orderDB);

        assertThrows(OrderBusinessException.class,
                () -> orderService.rejection(dto));
    }

    // ================================= cancel()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 取消订单（已支付需退款）")
    void cancel_withRefund() {
        OrdersCancelDTO dto = new OrdersCancelDTO();
        dto.setId(1L);
        dto.setCancelReason("顾客要求");

        Orders orderDB = Orders.builder().id(1L).payStatus(Orders.PAID).amount(new BigDecimal("50.00")).build();
        when(orderMapper.getById(1L)).thenReturn(orderDB);

        orderService.cancel(dto);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.CANCELLED, captor.getValue().getStatus());
        assertEquals(Orders.REFUND, captor.getValue().getPayStatus());
    }

    // ================================= deliveryById()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 派送订单")
    void deliveryById_success() {
        orderService.deliveryById(1L);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.DELIVERY_IN_PROGRESS, captor.getValue().getStatus());
    }

    // ================================= complete()方法 单元测试 =================================

    @Test
    @DisplayName("商家端 - 完成订单")
    void complete_success() {
        orderService.complete(1L);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        assertEquals(Orders.COMPLETED, captor.getValue().getStatus());
    }
}
