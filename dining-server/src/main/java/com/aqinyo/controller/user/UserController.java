package com.aqinyo.controller.user;

import com.aqinyo.constant.JwtClaimsConstant;
import com.aqinyo.dto.UserLoginDTO;
import com.aqinyo.entity.User;
import com.aqinyo.properties.JwtProperties;
import com.aqinyo.result.Result;
import com.aqinyo.service.UserService;
import com.aqinyo.utils.JwtUtil;
import com.aqinyo.vo.UserLoginVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/*    C端  微信登录  Controller层   */

@RestController
@RequestMapping("user/user")
@Api(tags = "C端用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /*   微信登录   */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信登录：{}", userLoginDTO.getCode());

        // 调用service层的微信登录
        User user = userService.wxlogin(userLoginDTO);

        // 登录成功后,为微信用户 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());// 把用户主键值放进去 (然后下面就把生成的JWT令牌 绑定 给这个id的用户)
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        // 把要返回的id、openid、token封装到VO类中返回前端 (然后"用户"就能有token进入程序做操作了)
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)   //设置token: 是上面自己写的JwtUtil工具类中的createJWT方法,创建出来的token
                .build();

        return Result.success(userLoginVO); //返回VO类 (里面封装的是id、openid、token这三)
    }

}
