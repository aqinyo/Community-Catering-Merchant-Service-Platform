package com.aqinyo.annotation;

import com.aqinyo.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*   自定义注解@AutoFill: 用于 "标识" 某个方法需要进行功能字段自动填充处理 (仅标识公共的部分出来统一做AOP增强的)  */

@Target(ElementType.METHOD)   //指定注解只能加在"方法method"上
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //指定数据库的操作类型: update、insert     (可以说 @Target + OperationType类型 --> 都是 限制这个自定义注解的使用范围)
    OperationType value();
}
