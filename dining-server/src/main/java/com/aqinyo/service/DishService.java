package com.aqinyo.service;

import com.aqinyo.dto.DishDTO;
import com.aqinyo.dto.DishPageQueryDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.DishVO;

import java.util.List;

public interface DishService {

    // 新增菜品
    void addDishWithFlavor(DishDTO dishDTO);

    // 分页查询
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    // 批量删除
    void deleteBatch(List<Long> ids);

    // 根据id查询 菜品和对应口味
    DishVO getByIdWithFlavor(Long id);

    // 根据id修改 菜品和对应的口味
    void updateDishWithFlavor(DishDTO dishDTO);

    // 启用禁用
    void startOrStop(int status, Long id);

    // 查询分类id
    List<Dish> getByCategoryId(Long categoryId);
    List<DishVO> listWithFlavor(Dish dish);

}
