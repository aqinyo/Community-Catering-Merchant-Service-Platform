package com.aqinyo.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.constant.StatusConstant;
import com.aqinyo.dto.CategoryDTO;
import com.aqinyo.dto.CategoryPageQueryDTO;
import com.aqinyo.entity.Category;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.mapper.CategoryMapper;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*   分类管理 Service层    */

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;  // 分类       (多态写法-->依赖注入的是接口实现类的对象)
    @Autowired
    private DishMapper dishMapper; // 菜品
    @Autowired
    private SetmealMapper setmealMapper;    // 菜品套餐


    /*   新增分类   */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);    // 与新增员工一样嘛,也是把前端发来DTO类数据--转换-->实体类(剩下的属性就手动set)

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        //设置创建时间、修改时间、创建人、修改人
        /*   可以通过AOP切面类-->实现公共字段自动赋值,无需手动赋值 (指四个公共的部分: CreateTime，UpdateTime、CreateUser的ID、UpdateUser的ID)   */
        //category.setCreateTime(LocalDateTime.now());
        //category.setUpdateTime(LocalDateTime.now());
        //category.setCreateUser(BaseContext.getCurrentId());
        //category.setUpdateUser(BaseContext.getCurrentId());

        categoryMapper.insert(category);
    }


    /*   分页查询   */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分页，自动加入limit关键字分页
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /*   删除分类 (根据id)   */
    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        Integer count = dishMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        count = setmealMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
    }


    /*   修改分类 (根据已存在的id修改)  */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        /*   可以通过AOP切面类-->实现公共字段自动赋值,无需手动赋值 (指四个公共的部分: CreateTime，UpdateTime、CreateUser的ID、UpdateUser的ID)   */
        //设置修改时间、修改人
        //category.setUpdateTime(LocalDateTime.now());
        //category.setUpdateUser(BaseContext.getCurrentId());

        categoryMapper.update(category);
    }


    /*   启用、禁用分类   */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                //.updateTime(LocalDateTime.now())
                //.updateUser(BaseContext.getCurrentId())
                .build();
        categoryMapper.update(category);
    }


    /*   查询分类 (根据类型)  */
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }

}
