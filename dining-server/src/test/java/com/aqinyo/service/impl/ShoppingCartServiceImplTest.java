package com.aqinyo.service.impl;

import com.aqinyo.context.BaseContext;
import com.aqinyo.dto.ShoppingCartDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.entity.ShoppingCart;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.mapper.ShoppingCartMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private SetmealMapper setmealMapper;

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    // ==================== addShoppingCart 方法测试 ====================

    @Test
    @DisplayName("添加购物车 - 商品已存在，数量+1")
    void addShoppingCart_itemExists() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);
        dto.setDishFlavor("微辣");

        ShoppingCart existingCart = ShoppingCart.builder()
                .id(1L).dishId(1L).number(2).amount(new BigDecimal("38.00")).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(existingCart)));

        shoppingCartService.addShoppingCart(dto);

        // 验证调用了 updateNumberById（数量+1），而非 insert
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).updateNumberById(captor.capture());
        assertEquals(3, captor.getValue().getNumber());  // 原来2，+1后为3
        verify(shoppingCartMapper, never()).insert(any());
    }

    @Test
    @DisplayName("添加购物车 - 新增菜品")
    void addShoppingCart_newDish() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);

        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.emptyList());

        Dish dish = Dish.builder()
                .id(1L).name("宫保鸡丁").price(new BigDecimal("38.00")).image("/img/1.png").build();
        when(dishMapper.getById(1L)).thenReturn(dish);

        shoppingCartService.addShoppingCart(dto);

        // 验证调用了 insert（新增），而非 updateNumberById
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).insert(captor.capture());

        ShoppingCart captured = captor.getValue();
        assertEquals("宫保鸡丁", captured.getName());
        assertEquals(new BigDecimal("38.00"), captured.getAmount());
        assertEquals(1, captured.getNumber());
        assertNotNull(captured.getCreateTime());
        verify(shoppingCartMapper, never()).updateNumberById(any());
    }

    @Test
    @DisplayName("添加购物车 - 新增套餐")
    void addShoppingCart_newSetmeal() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setSetmealId(1L);
        // dishId 为 null，走套餐分支

        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.emptyList());

        Setmeal setmeal = Setmeal.builder()
                .id(1L).name("双人套餐").price(new BigDecimal("99.00")).image("/img/s1.png").build();
        when(setmealMapper.getById(1L)).thenReturn(setmeal);

        shoppingCartService.addShoppingCart(dto);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).insert(captor.capture());

        ShoppingCart captured = captor.getValue();
        assertEquals("双人套餐", captured.getName());
        assertEquals(new BigDecimal("99.00"), captured.getAmount());
        assertEquals(1, captured.getNumber());
    }

    // ==================== showShoppingCart 方法测试 ====================

    @Test
    @DisplayName("查询购物车 - 正常返回")
    void showShoppingCart_success() {
        List<ShoppingCart> carts = Arrays.asList(
                ShoppingCart.builder().id(1L).name("宫保鸡丁").number(2).build()
        );
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(carts);

        List<ShoppingCart> result = shoppingCartService.showShoppingCart();

        assertEquals(1, result.size());
        assertEquals("宫保鸡丁", result.get(0).getName());
    }

    // ==================== cleanShoppingCart 方法测试 ====================

    @Test
    @DisplayName("清空购物车")
    void cleanShoppingCart_success() {
        shoppingCartService.cleanShoppingCart();

        verify(shoppingCartMapper, times(1)).clean(1L);
    }

    // ==================== subShoppingCart 方法测试 ====================

    @Test
    @DisplayName("减少购物车 - 数量为1时删除记录")
    void subShoppingCart_deleteWhenOne() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);

        ShoppingCart cart = ShoppingCart.builder().id(1L).dishId(1L).number(1).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(cart)));

        shoppingCartService.subShoppingCart(dto);

        // 数量为1，直接删除
        verify(shoppingCartMapper, times(1)).delete(1L);
        verify(shoppingCartMapper, never()).updateNumberById(any());
    }

    @Test
    @DisplayName("减少购物车 - 数量>1时减1")
    void subShoppingCart_decreaseNumber() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);

        ShoppingCart cart = ShoppingCart.builder().id(1L).dishId(1L).number(3).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(cart)));

        shoppingCartService.subShoppingCart(dto);

        // 数量>1，减1后更新
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).updateNumberById(captor.capture());
        assertEquals(2, captor.getValue().getNumber());
        verify(shoppingCartMapper, never()).delete(anyLong());
    }
}
