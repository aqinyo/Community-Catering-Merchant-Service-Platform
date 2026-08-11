package com.aqinyo.controller.user;

import com.aqinyo.entity.Category;
import com.aqinyo.result.Result;
import com.aqinyo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/*    C端  分类管理  Controller层    */

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Tag(name = "user端-分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /*   查询分类   */
    @GetMapping("/list")
    @Operation(summary = "查询分类")
    public Result<List<Category>> list(Integer type) {
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

}
