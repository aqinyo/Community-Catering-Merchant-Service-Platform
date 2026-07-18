package com.aqinyo.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.dto.SetmealDTO;
import com.aqinyo.dto.SetmealPageQueryDTO;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.entity.SetmealDish;
import com.aqinyo.exception.DeletionNotAllowedException;
import com.aqinyo.exception.SetmealEnableFailedException;
import com.aqinyo.mapper.DishMapper;
import com.aqinyo.mapper.SetmealDishMapper;
import com.aqinyo.mapper.SetmealMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.service.SetmealService;
import com.aqinyo.vo.DishItemVO;
import com.aqinyo.vo.DishVO;
import com.aqinyo.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery();
        long total = page.getTotal();
        List<Setmeal> list = page.getResult();
        return new PageResult(total, list);
    }

    @Override
    @Transactional
    public void addSetmealWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        if(dishes != null && !dishes.isEmpty()){
            dishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
            setmealDishMapper.insertBatch(dishes);
        }
    }

    @Override
    public void updateSetmealWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();

        if(dishList != null && !dishList.isEmpty()){
            dishList.forEach(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()));
            setmealDishMapper.insertBatch(dishList);
        }
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);

        List<SetmealDish> dishes = setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(dishes);
        return setmealVO;
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == 1){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        setmealMapper.deleteByIds(ids);
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    @Override
    public void startOrStop(int status, Long id) {
        if(status == 1){
            int count = setmealDishMapper.stopDishCount(id);
            if(count > 0) throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
