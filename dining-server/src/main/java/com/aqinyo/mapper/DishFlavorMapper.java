package com.aqinyo.mapper;

import com.aqinyo.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    // 批量插入口味
    void insertBatch(List<DishFlavor> flavors);

    // 删除口味 (根据菜品id)   XML版
    void deleteByDishIds(List<Long> dishIds);

    // 删除口味 (根据菜品id)  注解版
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    // 查询 (根据菜品id)
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);

}
