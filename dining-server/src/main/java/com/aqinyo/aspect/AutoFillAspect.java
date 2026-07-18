package com.aqinyo.aspect;

import com.aqinyo.annotation.AutoFill;
import com.aqinyo.constant.AutoFillConstant;
import com.aqinyo.context.BaseContext;
import com.aqinyo.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/*   自定义切面类: 实现公共字段自动填充处理的业务逻辑   */

@Aspect     // 做AOP切面注解
@Component
@Slf4j
public class AutoFillAspect {

    // 设置切入点
    @Pointcut("execution(* com.aqinyo.mapper.*.*(..)) && @annotation(com.aqinyo.annotation.AutoFill)")  // 拦截到mapper包下的所有类和方法 and 使用到@annotation注解
    public void autoFillPointCut(){}    // 切入点的定义依托一个私有的、不具有实际意义的方法进行,即无参数、无返回值

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("开始进行公共字段的填充....");

        //获取当前被拦截的方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//获取方法签名对象
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);//获取方法上的注解对象
        OperationType operationType = annotation.value();//获取数据库的操作类型

        //获取当前被拦截的方法的参数-->实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;
        Object entity = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        long id = BaseContext.getCurrentId();

        //根据当前不同的数据类型,为对应的属性通过反射来赋值
        if (operationType == OperationType.INSERT){
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射为对象赋值
                setUpdateUser.invoke(entity, id);
                setCreateUser.invoke(entity, id);
                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);

                setUpdateUser.invoke(entity, id);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
