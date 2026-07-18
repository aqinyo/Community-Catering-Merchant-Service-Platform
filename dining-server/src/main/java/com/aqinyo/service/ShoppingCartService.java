package com.aqinyo.service;

import com.aqinyo.dto.ShoppingCartDTO;
import com.aqinyo.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    // 添加商品 进购物车
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    // 动态条件查询   (条件比如有:用户id + 套餐id + 菜品id + 菜品口味)
    List<ShoppingCart> showShoppingCart();

    // 清空购物车
    void cleanShoppingCart();

    // 删除购物车某个菜品
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
