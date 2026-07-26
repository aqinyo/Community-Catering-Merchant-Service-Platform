package com.aqinyo.service;

import com.aqinyo.dto.CategoryDTO;
import com.aqinyo.dto.CategoryPageQueryDTO;
import com.aqinyo.entity.Category;
import com.aqinyo.result.PageResult;
import java.util.List;

public interface CategoryService {

    /*   用户端   */
    // 查询分类 (根据类型的id)
    List<Category> list(Integer type);


    /*   商家端   */
    // 新增分类
    void save(CategoryDTO categoryDTO);

    // 分页查询
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    // 删除分类 (根据id)
    void deleteById(Long id);

    // 修改分类 ((根据已存在的id修改))
    void update(CategoryDTO categoryDTO);

    // 启用、禁用分类
    void startOrStop(Integer status, Long id);

}
