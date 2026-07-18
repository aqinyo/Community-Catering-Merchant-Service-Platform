package com.aqinyo.service.impl;

import com.aqinyo.context.BaseContext;
import com.aqinyo.dto.ShoppingCartDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.entity.ShoppingCart;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.mapper.ShoppingCartMapper;
import com.aqinyo.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper; //下面判断加入购物车是菜品/套餐会用到
    @Autowired
    private SetmealMapper setmealMapper; //同理


    /*   添加商品 进购物车   (三步走)  */
    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 1.判断当前加入到购物车中的商品是否已经存在了？
        ShoppingCart shoppingCart = new ShoppingCart(); // new的空对象要赋值后再传给mapper去执行SQL
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);  // 依旧是对象属性拷贝: DTO-->实体类
        Long userId = BaseContext.getCurrentId(); // 获取当前用户id,然后结合下面的set手动赋值 "DTO类没有的属性且当前需要的属性" 到实体类中
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 2.如果已经存在了,只需要将数量+1
        if(list != null && !list.isEmpty()){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1); // 在原先的数量上+1
            shoppingCartMapper.updateNumberById(cart);  // 然后调用mapper去执行SQL修改数量
        }else {
            // 3.如果不存在,需要插入一条购物车数据
            /* 情况:因为不知道添加的是菜品还是套餐; 处理:所以要先判断  (这里学的主要是一个思路和逻辑,知道什么样的情况怎么样去设计代码处理) */
            if (shoppingCart.getDishId() != null){
                // 判断得出-->"本次"添加的是菜品
                Dish dish = dishMapper.getById(shoppingCart.getDishId());//mapper查询数据库查到有这个菜品id说明就是添加菜品的
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());    // 然后get菜品的信息出来设置到购物车对象里(属于是手动赋值的范畴)
            }else {
                // 判断得出-->"本次"添加的是套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            shoppingCart.setNumber(1); //这两个共同的抽取出来-->即无论你加入的是菜品/套餐-->都是要数量+1
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCart);//然后调用mapper方法把 带有数据的购物车对象 传过去执行SQL即可
        }
    }


    /*   动态查询 购物车菜品   */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .id(BaseContext.getCurrentId())
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }


    /*   清空购物车   */
    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.clean(BaseContext.getCurrentId());
    }


    /*   删除购物车某个购物车   */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        ShoppingCart cart = list.get(0);
        if(cart.getNumber() == 1){
            shoppingCartMapper.delete(list.get(0).getId());
        }else {
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.updateNumberById(cart);
        }
    }

}
