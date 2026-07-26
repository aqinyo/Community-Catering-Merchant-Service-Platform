package com.aqinyo.service;

import com.aqinyo.dto.SetmealDTO;
import com.aqinyo.dto.SetmealPageQueryDTO;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.DishItemVO;
import com.aqinyo.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /*   用户端   */
    // 条件查询
    List<Setmeal> list(Setmeal setmeal);

    // 根据套餐id查询包含的菜品
    List<DishItemVO> getDishItemById(Long id);


    /*   商家端   */
    // 分页查询
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    // 添加套餐
    void addSetmealWithDish(SetmealDTO setmealDTO);

    // 修改套餐
    void updateSetmealWithDish(SetmealDTO setmealDTO);

    // 根据id查询套餐
    SetmealVO getByIdWithDish(Long id);

    // 批量删除套餐
    void deleteBatch(List<Long> ids);

    // 启用禁用套餐
    void startOrStop(int status, Long id);

}
