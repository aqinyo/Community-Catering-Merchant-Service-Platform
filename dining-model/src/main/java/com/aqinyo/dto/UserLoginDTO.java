package com.aqinyo.dto;

import lombok.Data;

import java.io.Serializable;

/*   C端用户登录   */
@Data   // 封装注解
public class UserLoginDTO implements Serializable {

    private String code;

}
