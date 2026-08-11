package com.aqinyo.controller.user;

import com.aqinyo.dto.ShoppingCartDTO;
import com.aqinyo.entity.ShoppingCart;
import com.aqinyo.result.Result;
import com.aqinyo.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*    C端  购物车  controller层   */

@RestController
@RequestMapping("user/shoppingCart")
@Slf4j
@Tag(name = "user端-购物车相关接口")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /*   添加商品 进购物车   */
    @PostMapping("/add")
    @Operation(summary = "添加商品进购物车")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加商品：{}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /*   查看购物车   */
    @GetMapping("/list")
    @Operation(summary = "查看购物车")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车");
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }

    /*   清空购物车   */
    @DeleteMapping("/clean")
    @Operation(summary = "清空购物车")
    public Result<String> clean(){
        log.info("清空购物车");
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /*   删除 购物车某个商品   */
    @PostMapping("/sub")
    @Operation(summary = "删除商品")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("删除商品：{}", shoppingCartDTO);
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }

}
