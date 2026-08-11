package com.aqinyo.task;

import com.aqinyo.constant.StatusConstant;
import com.aqinyo.entity.Category;
import com.aqinyo.entity.Dish;
import com.aqinyo.entity.Setmeal;
import com.aqinyo.result.Result;
import com.aqinyo.service.CategoryService;
import com.aqinyo.service.DishService;
import com.aqinyo.service.SetmealService;
import com.aqinyo.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/*
 *  定时任务: Redis缓存预热
 *    预热范围: C端菜品缓存(手动式缓存) + C端套餐缓存(注解式缓存)                                     (两者都是:按分类id拆开缓存)
 *    预热策略: 【必要】@PostConstruct服务启动预热  +  【核心】@Scheduled(cron)凌晨定时刷新预热        (两者的代码逻辑一样,仅是注解不同,发挥的作用不同而已)
 */

@Slf4j
@Component
public class RedisTask {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;        // 手动式缓存
    @Autowired
    private SetmealService setmealService;  // 注解式缓存
    @Autowired
    private RedisTemplate redisTemplate;    // 自定义的RedisTemplate模板对象
    @Autowired
    private CacheManager cacheManager;      // 自定义的CacheManager缓存管理器对象


    /*   【必要】服务启动预热: 服务启动后执行一次预热     (应对服务版本迭代的重新发布,也避免了服务重启出现缓存雪崩)   */
    @PostConstruct      // @PostConstruct: 确保该方法在依赖注入后,立即执行该方法              (在部署中我去掉了该注解，因为容器启动太快，Mapper来不及初始化就报错)
    public void warmUpOnStartup() {
        log.info("【缓存预热】服务启动, 开始预热Redis缓存..."); //目的:避免服务冷启动时,大量请求打穿数据库
        warmUpDishCache();      //预热菜品缓存
        warmUpSetmealCache();   //预热套餐缓存
        log.info("【缓存预热】服务启动预热完成 ✓");
    }

    /*   【核心】凌晨定时刷新预热: 每日凌晨2点定时刷新    (应对商家白天修改了菜品/套餐数据, 凌晨统一刷新缓存,保证次日缓存为最新)   */
    @Scheduled(cron = "0 0 2 * * ?")    //在我RedisTask类中,只有这里使用了@Scheduled的,才是发挥了Spring Task定时任务的作用
    public void warmUpDaily() {
        log.info("【缓存预热】定时任务触发, 开始刷新Redis缓存...");
        warmUpDishCache();      //预热菜品缓存
        warmUpSetmealCache();   //预热套餐缓存
        log.info("【缓存预热】凌晨定时刷新预热完成 ✓");
    }




    /*   预热菜品缓存  (手动式缓存)   */
    private void warmUpDishCache() {
        // 查询所有"菜品类型"的分类 (type=1)
        List<Category> categoryType = categoryService.list(1);  // 1、菜品分类 2、套餐分类

        // 非空校验:分类为空 --> 跳过
        if (categoryType == null || categoryType.isEmpty()) {
            log.warn("【缓存预热】无菜品分类数据, 跳过菜品预热");
            return;
        }

        // 增强for循环:遍历"菜品分类",预热该菜品分类下的菜品
        for (Category category : categoryType) {    // 含义:对于categoryType中的每一个category
            //【异常兜底】给循环预热加try-catch, 避免某个分类预热时出现异常,导致整个菜品预热任务中断
            try {
                String key = "Dish_categoryId=" + category.getId();//与C端查询时:构造redis中key的规则保持一致
                Dish dish = new Dish();
                dish.setCategoryId(category.getId());
                dish.setStatus(StatusConstant.ENABLE);
                List<DishVO> dishVOList = dishService.listWithFlavor(dish);
                redisTemplate.opsForValue().set(key, dishVOList); //手动式缓存预热:set进Redis (因为C端缓存菜品使用手动式缓存,所以这里预热也是手动;下面的注解式同理)
            } catch (Exception e) {                               /* 这是"机制绑定": 读缓存用哪套工具,预热就必须用哪套工具写入 */
                log.error("【缓存预热】菜品分类[{}]预热失败: {}", category.getId(), e.getMessage(), e);
            }
        }
        log.info("【缓存预热】菜品缓存预热完成, 共 {} 个分类", categoryType.size());
    }



    /*   预热套餐缓存  (注解式缓存)   */
    private void warmUpSetmealCache() {
        // 查询所有"套餐类型"的分类 (type=2)
        List<Category> categoryType = categoryService.list(2);  // 1、菜品分类 2、套餐分类

        // 非空校验:分类为空 --> 跳过
        if (categoryType == null || categoryType.isEmpty()) {
            log.warn("【缓存预热】无套餐分类数据, 跳过套餐预热");
            return;
        }

        // 获取缓存空间  (对应 @Cacheable(cacheNames="SetMeal") 的那个空间)
        Cache cache = cacheManager.getCache("SetMeal");
        // 非空校验: 缓存空间不存在 --> 跳过
        if (cache == null) {
            log.warn("【缓存预热】未找到SetMeal缓存空间, 跳过套餐预热");
            return;
        }

        // 增强for循环:遍历"套餐分类",预热该套餐分类下的套餐
        for (Category category : categoryType) {
            //【异常兜底】给下面for循环的预热加try-catch, 避免某个套餐预热时出现异常,导致整个套餐预热任务中断
            try {
                Setmeal setmeal = new Setmeal();
                setmeal.setCategoryId(category.getId());
                setmeal.setStatus(StatusConstant.ENABLE);
                List<Setmeal> setmealList = setmealService.list(setmeal);
                cache.put(category.getId(), Result.success(setmealList));   // 缓存预热存入的 要与 "@Cacheable缓存的那个方法的return值" 的类型保持一致
            } catch (Exception e) {                                         // 这里用cache.put(key, value)是通过Spring Cache的CacheManager抽象来操作的,所以叫"注解式缓存"
                log.error("【缓存预热】套餐分类[{}]预热失败: {}", category.getId(), e.getMessage(), e);
            }
        }
        log.info("【缓存预热】套餐缓存预热完成, 共 {} 个分类", categoryType.size());
    }

}
