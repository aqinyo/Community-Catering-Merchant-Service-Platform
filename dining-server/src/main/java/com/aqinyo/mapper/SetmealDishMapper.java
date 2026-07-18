package com.aqinyo.mapper;

import com.aqinyo.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    // 根据菜品id查询 --> 对应的套餐id
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> dishes);

    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId};")
    void deleteBySetmealId(Long setmealId);

    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    void deleteBySetmealIds(List<Long> setmealIds);

    int stopDishCount(Long SetmealId);

}
