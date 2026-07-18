package com.aqinyo.service;

import com.aqinyo.dto.SetmealDTO;
import com.aqinyo.dto.SetmealPageQueryDTO;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.result.PageResult;
import com.aqinyo.vo.DishItemVO;
import com.aqinyo.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void addSetmealWithDish(SetmealDTO setmealDTO);

    void updateSetmealWithDish(SetmealDTO setmealDTO);

    SetmealVO getByIdWithDish(Long id);

    void deleteBatch(List<Long> ids);

    void startOrStop(int status, Long id);

    List<Setmeal> list(Setmeal setmeal);

    List<DishItemVO> getDishItemById(Long id);
}
