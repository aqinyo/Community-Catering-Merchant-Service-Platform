package com.aqinyo.controller.admin;

import com.aqinyo.dto.DishDTO;
import com.aqinyo.dto.DishPageQueryDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.result.PageResult;
import com.aqinyo.result.Result;
import com.aqinyo.service.DishService;
import com.aqinyo.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/*    菜品管理  Controller层   */

@RestController
@RequestMapping("admin/dish")
@Slf4j
@Api(tags = "admin端-菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishservice;//依赖注入
    @Autowired
    private RedisTemplate redisTemplate;    // 依赖注入的是自定义的RedisTemplate (也是Redis的java客户端-->连接并使用Redis服务端的)
                                            // 可用自定义模板redisTemplate(当前选用)  或  默认string类型的模板StringRedisTemplate


    /*   抽取删除Redis缓存方法: 全删菜品数据缓存   */
    private void CleanRedis(String pattern) {   // 因为只在本类中使用,所以是私有属性
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }


    /*  新增菜品  */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> add(@RequestBody DishDTO dishDTO){    // 依旧是 DTO类 接收前端请求发来的数据 (json数据则都用@RequestBody给形参加上)
        log.info("新增菜品：{}", dishDTO);

        // 精准删除 Redis旧的缓存数据
        String key = "Dish_categoryId=" + dishDTO.getCategoryId(); // key通过接收前端传数据过来的DTO类中,去get里面的分类id(然后用于下面的删除Redis缓存)
        CleanRedis(key);  // 删除Redis里面的缓存 (每次新增都是要执行)

        dishservice.addDishWithFlavor(dishDTO);
        return Result.success();
    }

    /*  分页查询菜品  */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){  // 依旧是 DTO类 接收前端请求发来的数据
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishservice.pageQuery(dishPageQueryDTO);/* controller层是调用service层的方法去进行业务处理操作的 (处理DTO类数据) */
        return Result.success(pageResult);
    }

    /*  批量删除菜品  */
    @DeleteMapping
    @ApiOperation("菜品批量删除")     // 加@RequestParam因为是动态删除,单个也可以删
    public Result<String> delete(@RequestParam List<Long> ids){   // 采用list集合是因为前端请求涉及多个参数
        log.info("菜品批量删除：{}", ids);

        // 删除redis中全部缓存的菜品数据
        CleanRedis("Dish_*");

        dishservice.deleteBatch(ids);
        return Result.success();
    }

    /*  查询菜品 (根据id)  */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishservice.getByIdWithFlavor(id);  // 把查询的数据封装为VO类返回给前端
        return Result.success(dishVO);
    }

    /*  查询菜品 (根据分类id)  */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(Long categoryId){
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> dishList = dishservice.getByCategoryId(categoryId);
        return Result.success(dishList);
    }

    /*  修改菜品  */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result<String> update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);

        // 删除redis中全部缓存的菜品数据
        CleanRedis("Dish_*");

        dishservice.updateDishWithFlavor(dishDTO);
        return Result.success();
    }

    /*  启用、禁用菜品  */
    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用菜品")
    public Result<String> startOrStop(@PathVariable("status") int status, Long id){
        log.info("启用或禁用菜品：{}, {}",status, id);

        // 删除redis中全部缓存的菜品数据
        CleanRedis("Dish_*");
        
        dishservice.startOrStop(status, id);
        return Result.success();
    }

}
