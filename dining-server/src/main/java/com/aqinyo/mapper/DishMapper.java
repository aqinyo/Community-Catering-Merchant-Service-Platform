package com.aqinyo.mapper;

import com.github.pagehelper.Page;
import com.aqinyo.annotation.AutoFill;
import com.aqinyo.dto.DishPageQueryDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.enumeration.OperationType;
import com.aqinyo.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/*   菜品管理 Mapper层   */

@Mapper
public interface DishMapper {

    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    // 新增菜品
    @AutoFill(OperationType.INSERT)
    void insert(Dish dish);

    // 分页查询
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    // 删除菜品 (根据id)
    void deleteByIds(List<Long> ids);

    // 修改菜品
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    List<Dish> getByCategoryId(Long categoryId);

    List<Dish> list(Dish dish);

    /*  根据条件统计菜品数量  */
    Integer countByMap(Map map);

}
