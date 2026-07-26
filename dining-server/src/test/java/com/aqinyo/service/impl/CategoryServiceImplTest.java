package com.aqinyo.service.impl;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.constant.StatusConstant;
import com.aqinyo.dto.CategoryDTO;
import com.aqinyo.dto.CategoryPageQueryDTO;
import com.aqinyo.entity.Category;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.mapper.CategoryMapper;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.result.PageResult;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private SetmealMapper setmealMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;


    // ==================== save 方法测试 ====================
    @Test
    @DisplayName("新增分类 - 默认状态为禁用")
    void save_success() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("川菜");
        dto.setType(1);
        dto.setSort(1);

        categoryService.save(dto);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());

        Category captured = captor.getValue();
        assertEquals("川菜", captured.getName());
        assertEquals(StatusConstant.DISABLE, captured.getStatus());  // 新增分类默认为禁用
    }

    // ==================== pageQuery 方法测试 ====================
    @Test
    @DisplayName("分页查询分类 - 正常返回")
    void pageQuery_success() {
        CategoryPageQueryDTO queryDTO = new CategoryPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        Page<Category> page = new Page<>(1, 10);
        page.setTotal(1);

        when(categoryMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult result = categoryService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    // ==================== deleteById 方法测试 ====================
    @Test
    @DisplayName("删除分类 - 正常删除（无关联菜品和套餐）")
    void deleteById_success() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(0);
        when(setmealMapper.countByCategoryId(1L)).thenReturn(0);

        categoryService.deleteById(1L);

        verify(categoryMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除分类 - 关联了菜品，抛出异常")
    void deleteById_relatedByDish() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(3);

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> categoryService.deleteById(1L));
        assertEquals(MessageConstant.CATEGORY_BE_RELATED_BY_DISH, exception.getMessage());
    }

    @Test
    @DisplayName("删除分类 - 关联了套餐，抛出异常")
    void deleteById_relatedBySetmeal() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(0);
        when(setmealMapper.countByCategoryId(1L)).thenReturn(2);

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> categoryService.deleteById(1L));
        assertEquals(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL, exception.getMessage());
    }

    // ==================== update 方法测试 ====================

    @Test
    @DisplayName("修改分类 - 正常修改")
    void update_success() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("改良川菜");
        dto.setType(1);
        dto.setSort(2);

        categoryService.update(dto);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).update(captor.capture());

        Category captured = captor.getValue();
        assertEquals("改良川菜", captured.getName());
        assertEquals(1L, captured.getId());
    }

    // ==================== startOrStop 方法测试 ====================

    @Test
    @DisplayName("启用分类")
    void startOrStop_enable() {
        categoryService.startOrStop(StatusConstant.ENABLE, 1L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).update(captor.capture());

        assertEquals(StatusConstant.ENABLE, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    @DisplayName("禁用分类")
    void startOrStop_disable() {
        categoryService.startOrStop(StatusConstant.DISABLE, 1L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).update(captor.capture());

        assertEquals(StatusConstant.DISABLE, captor.getValue().getStatus());
    }

    // ==================== list 方法测试 ====================

    @Test
    @DisplayName("根据类型查询分类列表")
    void list_success() {
        List<Category> categories = Arrays.asList(
                Category.builder().id(1L).name("川菜").type(1).build(),
                Category.builder().id(2L).name("粤菜").type(1).build()
        );
        when(categoryMapper.list(1)).thenReturn(categories);

        List<Category> result = categoryService.list(1);

        assertEquals(2, result.size());
        assertEquals("川菜", result.get(0).getName());
    }
}
