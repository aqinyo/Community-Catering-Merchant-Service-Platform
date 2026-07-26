package com.aqinyo.service.impl;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.constant.StatusConstant;
import com.aqinyo.dto.DishDTO;
import com.aqinyo.dto.DishPageQueryDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.entity.DishFlavor;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.mapper.DishFlavorMapper;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealDishMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.DishVO;
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
class DishServiceImplTest {

    @Mock
    private DishMapper dishMapper;
    @Mock
    private DishFlavorMapper dishFlavorMapper;
    @Mock
    private SetmealDishMapper setmealDishMapper;

    @InjectMocks
    private DishServiceImpl dishService;

    private Dish testDish;

    @BeforeEach
    void setUp() {
        testDish = Dish.builder()
                .id(1L)
                .name("宫保鸡丁")
                .categoryId(10L)
                .price(new BigDecimal("38.00"))
                .image("/images/dish1.png")
                .description("经典川菜")
                .status(StatusConstant.DISABLE)  // 默认停售，方便测试删除
                .build();
    }

    // ==================== addDishWithFlavor 方法测试 ====================

    @Test
    @DisplayName("新增菜品 - 带口味")
    void addDishWithFlavor_withFlavors() {
        DishDTO dto = new DishDTO();
        dto.setName("鱼香肉丝");
        dto.setCategoryId(10L);
        dto.setPrice(new BigDecimal("32.00"));
        dto.setImage("/images/dish2.png");

        // 构造口味数据
        DishFlavor flavor = DishFlavor.builder().name("辣度").value("[\"微辣\",\"中辣\",\"重辣\"]").build();
        dto.setFlavors(new ArrayList<>(Collections.singletonList(flavor)));

        dishService.addDishWithFlavor(dto);

        // 验证菜品 insert 被调用
        verify(dishMapper, times(1)).insert(any(Dish.class));
        // 验证口味批量插入被调用
        verify(dishFlavorMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("新增菜品 - 不带口味")
    void addDishWithFlavor_withoutFlavors() {
        DishDTO dto = new DishDTO();
        dto.setName("白米饭");
        dto.setCategoryId(10L);
        dto.setPrice(new BigDecimal("2.00"));
        dto.setFlavors(null);  // 无口味

        dishService.addDishWithFlavor(dto);

        verify(dishMapper, times(1)).insert(any(Dish.class));
        // 无口味时不应调用口味插入
        verify(dishFlavorMapper, never()).insertBatch(anyList());
    }

    // ==================== pageQuery 方法测试 ====================

    @Test
    @DisplayName("分页查询菜品 - 正常返回")
    void pageQuery_success() {
        DishPageQueryDTO queryDTO = new DishPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        Page<DishVO> page = new Page<>(1, 10);
        page.setTotal(1);

        when(dishMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult result = dishService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    // ==================== deleteBatch 方法测试 ====================

    @Test
    @DisplayName("批量删除菜品 - 正常删除（菜品均为停售状态且未被套餐关联）")
    void deleteBatch_success() {
        List<Long> ids = Arrays.asList(1L, 2L);

        // 两个菜品都是停售状态
        when(dishMapper.getById(1L)).thenReturn(testDish);
        when(dishMapper.getById(2L)).thenReturn(
                Dish.builder().id(2L).status(StatusConstant.DISABLE).build()
        );
        // 未被套餐关联
        when(setmealDishMapper.getSetmealIdsByDishIds(ids)).thenReturn(Collections.emptyList());

        dishService.deleteBatch(ids);

        verify(dishMapper, times(1)).deleteByIds(ids);
        verify(dishFlavorMapper, times(1)).deleteByDishIds(ids);
    }

    @Test
    @DisplayName("批量删除菜品 - 菜品起售中，抛出异常")
    void deleteBatch_dishOnSale() {
        List<Long> ids = Arrays.asList(1L);

        // 菜品处于起售状态
        Dish onSaleDish = Dish.builder().id(1L).status(StatusConstant.ENABLE).build();
        when(dishMapper.getById(1L)).thenReturn(onSaleDish);

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> dishService.deleteBatch(ids));
        assertEquals(MessageConstant.DISH_ON_SALE, exception.getMessage());
    }

    @Test
    @DisplayName("批量删除菜品 - 菜品被套餐关联，抛出异常")
    void deleteBatch_dishRelatedBySetmeal() {
        List<Long> ids = Arrays.asList(1L);

        when(dishMapper.getById(1L)).thenReturn(testDish);  // 停售状态
        // 被套餐关联
        when(setmealDishMapper.getSetmealIdsByDishIds(ids)).thenReturn(Arrays.asList(100L));

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> dishService.deleteBatch(ids));
        assertEquals(MessageConstant.DISH_BE_RELATED_BY_SETMEAL, exception.getMessage());
    }

    // ==================== getByIdWithFlavor 方法测试 ====================

    @Test
    @DisplayName("根据ID查询菜品和口味 - 正常返回")
    void getByIdWithFlavor_success() {
        List<DishFlavor> flavors = Arrays.asList(
                DishFlavor.builder().id(1L).dishId(1L).name("辣度").value("[\"微辣\",\"中辣\"]").build()
        );

        when(dishMapper.getById(1L)).thenReturn(testDish);
        when(dishFlavorMapper.getByDishId(1L)).thenReturn(flavors);

        DishVO result = dishService.getByIdWithFlavor(1L);

        assertNotNull(result);
        assertEquals("宫保鸡丁", result.getName());
        assertEquals(1, result.getFlavors().size());
        assertEquals("辣度", result.getFlavors().get(0).getName());
    }

    // ==================== updateDishWithFlavor 方法测试 ====================

    @Test
    @DisplayName("修改菜品和口味 - 有新口味")
    void updateDishWithFlavor_withNewFlavors() {
        DishDTO dto = new DishDTO();
        dto.setId(1L);
        dto.setName("宫保鸡丁(改良版)");
        dto.setPrice(new BigDecimal("42.00"));
        DishFlavor newFlavor = DishFlavor.builder().name("甜度").value("[\"少糖\",\"多糖\"]").build();
        dto.setFlavors(new ArrayList<>(Collections.singletonList(newFlavor)));

        dishService.updateDishWithFlavor(dto);

        // 验证：更新菜品 → 删除旧口味 → 插入新口味
        verify(dishMapper, times(1)).update(any(Dish.class));
        verify(dishFlavorMapper, times(1)).deleteByDishId(1L);
        verify(dishFlavorMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("修改菜品和口味 - 无口味")
    void updateDishWithFlavor_withoutFlavors() {
        DishDTO dto = new DishDTO();
        dto.setId(1L);
        dto.setName("宫保鸡丁");
        dto.setFlavors(null);

        dishService.updateDishWithFlavor(dto);

        verify(dishMapper, times(1)).update(any(Dish.class));
        verify(dishFlavorMapper, times(1)).deleteByDishId(1L);
        // 无口味时不应插入
        verify(dishFlavorMapper, never()).insertBatch(anyList());
    }

    // ==================== startOrStop 方法测试 ====================

    @Test
    @DisplayName("启用菜品")
    void startOrStop_enable() {
        dishService.startOrStop(StatusConstant.ENABLE, 1L);

        ArgumentCaptor<Dish> captor = ArgumentCaptor.forClass(Dish.class);
        verify(dishMapper).update(captor.capture());

        Dish captured = captor.getValue();
        assertEquals(StatusConstant.ENABLE, captured.getStatus());
        assertEquals(1L, captured.getId());
    }

    // ==================== getByCategoryId 方法测试 ====================

    @Test
    @DisplayName("根据分类ID查询菜品")
    void getByCategoryId_success() {
        List<Dish> dishes = Arrays.asList(testDish);
        when(dishMapper.getByCategoryId(10L)).thenReturn(dishes);

        List<Dish> result = dishService.getByCategoryId(10L);

        assertEquals(1, result.size());
        assertEquals("宫保鸡丁", result.get(0).getName());
    }

    // ==================== listWithFlavor 方法测试 ====================

    @Test
    @DisplayName("用户端 - 根据分类查询菜品和口味")
    void listWithFlavor_success() {
        Dish queryDish = Dish.builder().categoryId(10L).status(StatusConstant.ENABLE).build();
        List<Dish> dishList = Arrays.asList(testDish);
        List<DishFlavor> flavors = Arrays.asList(
                DishFlavor.builder().dishId(1L).name("辣度").value("[\"微辣\"]").build()
        );

        when(dishMapper.list(queryDish)).thenReturn(dishList);
        when(dishFlavorMapper.getByDishId(1L)).thenReturn(flavors);

        List<DishVO> result = dishService.listWithFlavor(queryDish);

        assertEquals(1, result.size());
        assertEquals("宫保鸡丁", result.get(0).getName());
        assertEquals(1, result.get(0).getFlavors().size());
    }

    @Test
    @DisplayName("用户端 - 查询菜品和口味（空列表）")
    void listWithFlavor_empty() {
        Dish queryDish = Dish.builder().categoryId(99L).build();
        when(dishMapper.list(queryDish)).thenReturn(Collections.emptyList());

        List<DishVO> result = dishService.listWithFlavor(queryDish);

        assertTrue(result.isEmpty());
        verify(dishFlavorMapper, never()).getByDishId(anyLong());
    }
}
