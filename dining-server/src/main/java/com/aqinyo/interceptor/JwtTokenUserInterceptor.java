package com.aqinyo.interceptor;

import com.aqinyo.constant.JwtClaimsConstant;
import com.aqinyo.context.BaseContext;
import com.aqinyo.properties.JwtProperties;
import com.aqinyo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*   用户端 jwt令牌校验的拦截器   */

@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;


    /*   校验JWT   (preHandle: 原始方法执行前拦截)  */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        /*   静态放行   */
        //判断当前拦截到的是Controller动态 / 静态资源
        if (!(handler instanceof HandlerMethod)) {
            return true;    //当前拦截到的不是controller动态方法,直接放行
        }

        /*   动态拦截   */
        //1、从“请求头Header”中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName()); // 没有token令牌就不不通过,即“未授权”之意

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);

            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);
            BaseContext.setCurrentId(userId);
            //3、通过放行
            return true;

        } catch (Exception ex) {
            //4、不通过,响应401状态码-->未授权
            response.setStatus(401);
            return false;   //false不放行
        }
    }

}
