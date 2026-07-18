package com.aqinyo.mapper;

import com.aqinyo.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    // 动态查询 购物车菜品信息
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    // 修改购物车菜品 数量
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart);

    // 插入购物车数据
    @Insert("insert into shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) " +
            "values (#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    // 清空购物车 (根据用户id)
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void clean(Long userId);

    // 删除购物车某个菜品(根据id)
    @Delete("delete from shopping_cart where id = #{id}")
    void delete(Long id);

    // 将购物车对象批量添加到数据库
    void insertBatch(List<ShoppingCart> shoppingCartList);

}
