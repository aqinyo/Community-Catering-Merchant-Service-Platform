package com.aqinyo.service.impl;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.dto.SetmealDTO;
import com.aqinyo.dto.SetmealPageQueryDTO;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.entity.SetmealDish;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.exception.SetmealEnableFailedException;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealDishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.DishItemVO;
import com.aqinyo.vo.SetmealVO;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class SetmealServiceImplTest {

    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private SetmealDishMapper setmealDishMapper;
    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private SetmealServiceImpl setmealService;

    private Setmeal testSetmeal;

    @BeforeEach
    void setUp() {
        testSetmeal = Setmeal.builder()
                .id(1L)
                .name("双人套餐")
                .categoryId(10L)
                .price(new BigDecimal("99.00"))
                .status(0)  // 停用状态
                .description("含宫保鸡丁、麻婆豆腐")
                .image("/images/setmeal1.png")
                .build();
    }

    // ================================= pageQuery()方法 单元测试 =================================

    @Test
    @DisplayName("分页查询套餐 - 正常返回")
    void pageQuery_success() {
        SetmealPageQueryDTO queryDTO = new SetmealPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        Page<Setmeal> page = new Page<>(1, 10);
        page.setTotal(1);

        when(setmealMapper.pageQuery()).thenReturn(page);

        PageResult result = setmealService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    // ================================= addSetmealWithDish()方法 单元测试 =================================

    @Test
    @DisplayName("新增套餐 - 带菜品")
    void addSetmealWithDish_withDishes() {
        SetmealDTO dto = new SetmealDTO();
        dto.setName("三人套餐");
        dto.setCategoryId(10L);
        dto.setPrice(new BigDecimal("139.00"));

        SetmealDish dish = SetmealDish.builder().dishId(1L).name("宫保鸡丁").copies(1).price(new BigDecimal("38.00")).build();
        dto.setSetmealDishes(new ArrayList<>(Collections.singletonList(dish)));

        setmealService.addSetmealWithDish(dto);

        verify(setmealMapper, times(1)).insert(any(Setmeal.class));
        verify(setmealDishMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("新增套餐 - 不带菜品")
    void addSetmealWithDish_withoutDishes() {
        SetmealDTO dto = new SetmealDTO();
        dto.setName("空套餐");
        dto.setCategoryId(10L);
        dto.setPrice(new BigDecimal("0.01"));
        dto.setSetmealDishes(null);

        setmealService.addSetmealWithDish(dto);

        verify(setmealMapper, times(1)).insert(any(Setmeal.class));
        verify(setmealDishMapper, never()).insertBatch(anyList());
    }

    // ================================= updateSetmealWithDish()方法 单元测试 =================================

    @Test
    @DisplayName("修改套餐 - 有新菜品关联")
    void updateSetmealWithDish_withDishes() {
        SetmealDTO dto = new SetmealDTO();
        dto.setId(1L);
        dto.setName("双人套餐(改良版)");
        dto.setPrice(new BigDecimal("109.00"));

        SetmealDish newDish = SetmealDish.builder().dishId(2L).name("麻婆豆腐").copies(1).price(new BigDecimal("28.00")).build();
        dto.setSetmealDishes(new ArrayList<>(Collections.singletonList(newDish)));

        setmealService.updateSetmealWithDish(dto);

        verify(setmealMapper, times(1)).update(any(Setmeal.class));
        verify(setmealDishMapper, times(1)).deleteBySetmealId(1L);
        verify(setmealDishMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("修改套餐 - 无菜品关联")
    void updateSetmealWithDish_withoutDishes() {
        SetmealDTO dto = new SetmealDTO();
        dto.setId(1L);
        dto.setName("双人套餐");
        dto.setSetmealDishes(null);

        setmealService.updateSetmealWithDish(dto);

        verify(setmealMapper, times(1)).update(any(Setmeal.class));
        verify(setmealDishMapper, times(1)).deleteBySetmealId(1L);
        verify(setmealDishMapper, never()).insertBatch(anyList());
    }

    // ================================= getByIdWithDish()方法 单元测试 =================================

    @Test
    @DisplayName("根据ID查询套餐和菜品 - 正常返回")
    void getByIdWithDish_success() {
        List<SetmealDish> dishes = Arrays.asList(
                SetmealDish.builder().id(1L).setmealId(1L).dishId(1L).name("宫保鸡丁").copies(1).build()
        );

        when(setmealMapper.getById(1L)).thenReturn(testSetmeal);
        when(setmealDishMapper.getBySetmealId(1L)).thenReturn(dishes);

        SetmealVO result = setmealService.getByIdWithDish(1L);

        assertNotNull(result);
        assertEquals("双人套餐", result.getName());
        assertEquals(1, result.getSetmealDishes().size());
    }

    // ================================= deleteBatch()方法 单元测试 =================================

    @Test
    @DisplayName("批量删除套餐 - 正常删除（均为停用状态）")
    void deleteBatch_success() {
        List<Long> ids = Arrays.asList(1L, 2L);

        when(setmealMapper.getById(1L)).thenReturn(testSetmeal);
        when(setmealMapper.getById(2L)).thenReturn(
                Setmeal.builder().id(2L).status(0).build()
        );

        setmealService.deleteBatch(ids);

        verify(setmealMapper, times(1)).deleteByIds(ids);
        verify(setmealDishMapper, times(1)).deleteBySetmealIds(ids);
    }

    @Test
    @DisplayName("批量删除套餐 - 套餐起售中，抛出异常")
    void deleteBatch_setmealOnSale() {
        List<Long> ids = Arrays.asList(1L);

        Setmeal onSaleSetmeal = Setmeal.builder().id(1L).status(1).build();
        when(setmealMapper.getById(1L)).thenReturn(onSaleSetmeal);

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> setmealService.deleteBatch(ids));
        assertEquals(MessageConstant.SETMEAL_ON_SALE, exception.getMessage());
    }

    // ================================= startOrStop()方法 单元测试 =================================

    @Test
    @DisplayName("禁用套餐")
    void startOrStop_disable() {
        setmealService.startOrStop(0, 1L);

        ArgumentCaptor<Setmeal> captor = ArgumentCaptor.forClass(Setmeal.class);
        verify(setmealMapper).update(captor.capture());

        Setmeal captured = captor.getValue();
        assertEquals(0, captured.getStatus());
        assertEquals(1L, captured.getId());
    }

    @Test
    @DisplayName("启用套餐 - 正常启用（无停售菜品）")
    void startOrStop_enable_success() {
        // 该套餐下没有停售菜品
        when(setmealDishMapper.stopDishCount(1L)).thenReturn(0);

        setmealService.startOrStop(1, 1L);

        ArgumentCaptor<Setmeal> captor = ArgumentCaptor.forClass(Setmeal.class);
        verify(setmealMapper).update(captor.capture());

        Setmeal captured = captor.getValue();
        assertEquals(1, captured.getStatus());
    }

    @Test
    @DisplayName("启用套餐 - 包含停售菜品，抛出异常")
    void startOrStop_enable_failed() {
        // 该套餐下有停售菜品
        when(setmealDishMapper.stopDishCount(1L)).thenReturn(2);

        SetmealEnableFailedException exception = assertThrows(SetmealEnableFailedException.class,
                () -> setmealService.startOrStop(1, 1L));
        assertEquals(MessageConstant.SETMEAL_ENABLE_FAILED, exception.getMessage());
    }

    // ================================= list()方法 单元测试 =================================

    @Test
    @DisplayName("用户端 - 条件查询套餐")
    void list_success() {
        Setmeal query = Setmeal.builder().categoryId(10L).status(1).build();
        List<Setmeal> setmeals = Arrays.asList(testSetmeal);
        when(setmealMapper.list(query)).thenReturn(setmeals);

        List<Setmeal> result = setmealService.list(query);

        assertEquals(1, result.size());
        assertEquals("双人套餐", result.get(0).getName());
    }

    // ================================= getDishItemById()方法 单元测试 =================================

    @Test
    @DisplayName("用户端 - 根据套餐ID查询包含的菜品")
    void getDishItemById_success() {
        List<DishItemVO> items = Arrays.asList(
                DishItemVO.builder().name("宫保鸡丁").copies(1).image("/images/dish1.png").build()
        );
        when(setmealMapper.getDishItemBySetmealId(1L)).thenReturn(items);

        List<DishItemVO> result = setmealService.getDishItemById(1L);

        assertEquals(1, result.size());
        assertEquals("宫保鸡丁", result.get(0).getName());
    }
}
