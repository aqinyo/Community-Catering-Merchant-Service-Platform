package com.aqinyo.handler;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.exception.BaseException;
import com.aqinyo.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/*
* 全局异常处理器,处理项目中抛出的业务异常
*/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /*  捕获业务异常  */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /*  处理SQL异常 --> 当输入已经存在的username时执行的拦截  */
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
