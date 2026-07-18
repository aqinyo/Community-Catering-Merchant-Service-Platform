package com.aqinyo.result;

import lombok.Data;

import java.io.Serializable;

/*
*   后端统一返回结果集
*   (可用理解为-->后端给前端的"信封",通过看简短的"code + msg + data"就能快速知道返回的情况-->而给前端的数据封装成的VO类,则是被data携带着)
*/

@Data
public class Result<T> implements Serializable {

    private Integer code;   //编码: 1成功 ; 0和其它数字为失败
    private String msg;     //错误信息
    private T data;         //返回给前端的数据

    public static <T> Result<T> success() {     // 无参success (不需要返回数据给前端)
        Result<T> result = new Result<>();
        result.code = 1;
        return result;
    }

    public static <T> Result<T> success(T object) { // 有参success (携带了要返回给前端的数据)
        Result<T> result = new Result<>();
        result.data = object;
        result.code = 1;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

}
