package com.aqinyo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aqinyo.constant.JwtClaimsConstant;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.dto.UserLoginDTO;
import com.aqinyo.entity.User;
import com.aqinyo.exception.LoginFailedException;
import com.aqinyo.mapper.UserMapper;
import com.aqinyo.properties.JwtProperties;
import com.aqinyo.properties.WeChatProperties;
import com.aqinyo.service.UserService;
import com.aqinyo.utils.HttpClientUtil;
import com.aqinyo.utils.JwtUtil;
import com.aqinyo.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*   用户登录 Service层   */

@Service
public class UserServiceImpl implements UserService {

    // 微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private WeChatProperties weChatProperties;

    /*   微信登录   */
    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {

        // 获取openid (使用的是下面抽取的方法)
        String openid = getString(userLoginDTO);

        // 判断openid是否为空,为空则抛出异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断当前用户是否为新用户(查询一下)
        User user = userMapper.getByOpenid(openid);

        // 如果是新用户,自动完成注册
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        return user;
    }

    /*   调用微信接口服务,获取当前用户的openid   */       // 抽取方法出来,上面就一行代码可用调用了,比较简洁可读,避免一大堆代码塞前面
    private String getString(UserLoginDTO userLoginDTO) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", userLoginDTO.getCode());//前端传过来的数据封装在DTO类里面了,直接调用get动态获取code即可
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);// 使用上之前封装底层代码的工具类了,能直接调用第三方接口使用

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }

}
