package com.aqinyo.mapper;

import com.github.pagehelper.Page;
import com.aqinyo.annotation.AutoFill;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.enumeration.OperationType;
import com.aqinyo.vo.DishItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    // 根据分类id查询套餐的数量
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    Page<Setmeal> pageQuery();

    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);

    void deleteByIds(List<Long> ids);

    List<Setmeal> list(Setmeal setmeal);

    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    // 根据条件统计套餐数量
    Integer countByMap(Map map);

}
