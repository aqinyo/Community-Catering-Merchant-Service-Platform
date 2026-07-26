package com.aqinyo.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.constant.StatusConstant;
import com.aqinyo.dto.DishDTO;
import com.aqinyo.dto.DishPageQueryDTO;
import com.aqinyo.entity.Dish;
import com.aqinyo.entity.DishFlavor;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.mapper.DishFlavorMapper;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealDishMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.service.DishService;
import com.aqinyo.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/*    菜品管理 Service层    */

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;  //菜品
    @Autowired
    private DishFlavorMapper dishFlavorMapper;  // 菜品口味
    @Autowired
    private SetmealDishMapper setmealDishMapper;    // 菜品套餐     (这个菜品是关联了两个表,所以要依赖注入两个 --> 菜品口味表 + 菜品套餐表)

    /*  商家端 - 新增菜品  */
    @Override            //加重写注解-->规范些(也可以省略掉,IDEA也能智能识别,但是加上规范些)
    @Transactional
    public void addDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);    // 属性拷贝(DTO-->dish实体类)

        dishMapper.insert(dish);// 由dishMapper去执行"菜品dish"的SQL

        //获取insert语句生成的"菜品主键值" (口味要依赖菜品id操作的)
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){ // 对口味进行判断 (用户也可能没有提交口味)
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));// 使用forEach遍历(Lambda表达式写法)是为了把每个口味都绑定个菜品id(因为一个菜品会有n个口味)
            dishFlavorMapper.insertBatch(flavors);// 由dishFlavorMapper去执行"口味flavors"的SQL
        }
    }


    /*  商家端 - 分页查询菜品  */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize()); // 用到PageHelper分页插件
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /*  商家端 - 批量删除菜品  */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除-->是否存在起售中的菜品?
        for(Long id : ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){  // 是否为启用？
                // 当前菜品处于起售中,不能删除,并抛提示信息
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断当前菜品是否能够删除-->是否被套餐关联？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);  // 使用到setmealDishMapper接口方法
        if(setmealIds != null && !setmealIds.isEmpty()){
            // 当前菜品被套餐关联,不能删除,并抛提示信息
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 删除菜品
        dishMapper.deleteByIds(ids);
        // 删除菜品 关联的口味
        dishFlavorMapper.deleteByDishIds(ids);
    }


    /*  商家端 - 根据id查询 菜品和对应口味  */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据菜品
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);//对象拷贝: 把"实体类"封装为"VO类"返回给前端
        dishVO.setFlavors(dishFlavors);//然后把剩余属性手动赋值(和DTO-->实体类一样的)
        return dishVO;
    }


    /*  商家端 - 根据id修改 菜品和对应口味  */
    @Override
    @Transactional
    public void updateDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);  // 一样是先对象属性拷贝转换一下

        // 修改菜品基本信息
        dishMapper.update(dish);

        // 先删除原本关联菜品的所有口味(再插入新加的口味)
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 获取新的口味然后循环插入
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
            // 向口味表 批量插入新口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }


    /*  商家端 - 启用、禁用菜品  */
    @Override
    public void startOrStop(int status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }


    /*   商家端 - 查询分类id  */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        return dishMapper.getByCategoryId(categoryId);
    }


    /*   用户端 - 根据分类id查询菜品和对应口味   */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}
