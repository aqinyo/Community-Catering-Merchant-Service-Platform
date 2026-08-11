package com.aqinyo.controller.user;

import com.aqinyo.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/*    C端  查询 店铺的营业状态   */

@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "user端-店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;  //依赖注入的是:自定义的RedisTemplate

    /*   查询 店铺的营业状态   */
    @GetMapping("/status")
    @Operation(summary = "查询店铺的营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY); // Spring Data Redis手动式 缓存至 Redis
        log.info("获取店铺的营业状态：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
