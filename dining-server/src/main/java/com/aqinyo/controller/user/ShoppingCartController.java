package com.aqinyo.controller.user;

import com.aqinyo.dto.ShoppingCartDTO;
import com.aqinyo.entity.ShoppingCart;
import com.aqinyo.result.Result;
import com.aqinyo.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*    C端  购物车  controller层   */

@RestController
@RequestMapping("user/shoppingCart")
@Slf4j
@Api(tags = "user端-购物车相关接口")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /*   添加商品 进购物车   */
    @PostMapping("/add")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加商品：{}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /*   查看购物车   */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车");
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }

    /*   清空购物车   */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result<String> clean(){
        log.info("清空购物车");
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /*   删除 购物车某个商品   */
    @PostMapping("/sub")
    @ApiOperation("删除商品")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("删除商品：{}", shoppingCartDTO);
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }

}
