package com.aqinyo.controller.admin;

import com.aqinyo.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/*    店铺营业状态管理  Controller层   */

@RestController("adminShopController")  //为了避免admin与user基于bean的名称都叫ShopController的同名冲突,多加东西来区分(其他解决方法: 就是改类名,不要两个都叫ShopController即可)
@RequestMapping("/admin/shop")
@Api(tags = "admin端-店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS"; //这样子把key单独列出来依旧是为了解耦和优雅

    @Autowired
    private RedisTemplate redisTemplate;    //依赖注入自定义Redis模板

    /*   设置 店铺的营业状态   */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺的营业状态")
    public Result<String> setStatus(@PathVariable int status){  //上面请求路径{status}带有{}是路径参数,要加@PathVariable注解 (/status这个就是普通字符串的请求路径)
        log.info("设置店铺营业状态：{}", status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY, status);   // 设置 key value (因为这个数据是存到Redis的)
        return Result.success();
    }

    /*   查询 店铺的营业状态   */    // 这块查询和用户的controller层的查询内容是一模一样的,仅仅是为了规范翻开写(细节是请求路径/admin、/user的区别)
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);    //强转的类型与上面设置的类型保持一致即可
        log.info("获取店铺的营业状态：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
