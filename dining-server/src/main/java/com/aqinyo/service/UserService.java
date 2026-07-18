package com.aqinyo.service;

import com.aqinyo.dto.UserLoginDTO;
import com.aqinyo.entity.User;
import com.aqinyo.vo.UserLoginVO;

public interface UserService {

    //微信登录
    User wxlogin(UserLoginDTO userLoginDTO);//因为是前端请求-->controller层-->再到server层,所以接口这里是接收DTO类

}
