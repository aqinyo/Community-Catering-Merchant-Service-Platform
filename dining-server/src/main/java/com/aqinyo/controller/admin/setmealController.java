package com.aqinyo.controller.admin;

import com.aqinyo.dto.SetmealDTO;
import com.aqinyo.dto.SetmealPageQueryDTO;
import com.aqinyo.result.PageResult;
import com.aqinyo.result.Result;
import com.aqinyo.service.SetmealService;
import com.aqinyo.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*    套餐管理  controller层
*   ( 因为商家端的操作基本是修改数据 --> 所以使用注解操作redis的基本都是@CacheEvict,用户那边基本都是@Cacheable/@CachePut )
*/

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class setmealController {

    @Autowired
    private SetmealService setmealService;

    /*   套餐分页查询   */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> setmealPageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /*   新增套餐   */
    @PostMapping
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")    //key: setmealCache::100...
    @ApiOperation("新增套餐")
    public Result<String> add(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}", setmealDTO);
        setmealService.addSetmealWithDish(setmealDTO);
        return Result.success();
    }

    /*   修改套餐   */
    @PutMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) // @CacheEvict删除缓存,allEntries=true 指删除setmealCache下的所有键值对(allEntries)
    @ApiOperation("修改套餐")
    public Result<String> update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}", setmealDTO);
        setmealService.updateSetmealWithDish(setmealDTO);
        return Result.success();
    }

    /*   根据id查询套餐   */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /*   批量删除套餐   */
    @DeleteMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) // @CacheEvict删除缓存,allEntries=true 指删除setmealCache下的所有键值对(allEntries)
    @ApiOperation("批量删除套餐")
    public Result<String> deleteByIds(@RequestParam List<Long> ids){
        log.info("批量删除套餐：{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /*   启用、禁用套餐   */
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) // @CacheEvict删除缓存,allEntries=true 指删除setmealCache下的所有键值对(allEntries)
    @ApiOperation("套餐起售或禁售")
    public Result<String> startOrStop(@PathVariable("status") int status, Long id){
        log.info("套餐的起售或禁售：{}, {}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

}
