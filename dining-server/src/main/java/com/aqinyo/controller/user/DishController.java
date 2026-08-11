package com.aqinyo.controller.user;

import com.aqinyo.constant.StatusConstant;
import com.aqinyo.entity.Dish;
import com.aqinyo.result.Result;
import com.aqinyo.service.DishService;
import com.aqinyo.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

/*    C端  查询菜品   (引入Redis: Spring Data Redis 手动式)   */

@RestController("userDishController")
@RequestMapping("/user/dish")
@Tag(name = "user端-菜品浏览接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;    //依赖注入的是:自定义的RedisTemplate

    /*   根据 "分类id" 查询菜品   */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        // 构造redis中key的规则: Dish_categoryId= --> 分类(key)下挂着该套餐对应的菜品(value)
        String key = "Dish_categoryId=" + categoryId;

        // 查询redis中是否存在菜品数据
        List<DishVO> dishVOList = (List<DishVO>) redisTemplate.opsForValue().get(key);// 放进去的数据类型 = 取出来的数据类型
        if(dishVOList != null && !dishVOList.isEmpty()){
            // 如果存在,直接返回,无须查询数据库
            return Result.success(dishVOList);
        }

        // 这里的实体类Dish仅作为封装数据库查询条件的数据载体
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        // 如果不存在,查询数据库,将查询到的数据放入redis中
        dishVOList = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key, dishVOList);

        return Result.success(dishVOList);
    }

}
