package com.aqinyo.controller.user;

import com.aqinyo.constant.StatusConstant;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.result.Result;
import com.aqinyo.service.SetmealService;
import com.aqinyo.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/*    C端  查询套餐   (引入Redis: Spring Cache 注解式)   */

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /*   条件查询   */
    @GetMapping("/list")                                            /*   注意:一般这个缓存注解写在service层的,这里偷懒写在controller层   */
    @Cacheable(cacheNames = "SetMeal", key = "#categoryId") //如果形参是user,则key写成#user.id     (最终效果是-->SetMeal::categoryId的值)
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        List<Setmeal> list = setmealService.list(setmeal);
        return Result.success(list);
    }

    /*   根据套餐id查询包含的菜品列表   */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }

}
