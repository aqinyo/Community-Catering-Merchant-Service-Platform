package com.aqinyo.handler;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.exception.BaseException;
import com.aqinyo.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/*
 *   全局异常处理器: 拦截项目中所有 "Controller层(即Web层)" 抛出的异常,统一处理后(统一序列化为JSON格式)返回友好的错误信息给前端 / 即return Result.error("xxx"),避免直接暴露堆栈信息
*/
@RestControllerAdvice // 基于该注解实现全局异常处理。该注解相当于 @ControllerAdvice + @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /*  捕获全局的异常 (最核心！)  */
    @ExceptionHandler   // @ExceptionHandler负责拦截 "指定异常类型" 并交给该方法处理
    public Result exceptionHandler(BaseException ex){   // 指定了BaseException的异常类型
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage()); //返回友好的错误信息给前端    (这就是想要达到:捕获全局异常的效果)
    }


    /*  处理SQL异常 (附带的异常捕获) --> 当输入已经存在的username时执行的拦截  */
    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException e){
        String massage = e.getMessage();
        if (massage.contains("Duplicate entry")){
            String[] massages = massage.split(" ");
            String msg = massages[2] + MessageConstant.ALREADY_EXIST;   //提示的信息都提取到通用模块的“信息提示常量类”中了,依旧是保持一个规范,去尽量避免直接写字符串
            return Result.error(msg);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
