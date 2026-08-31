package com.aqinyo.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.result.Result;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 *   Sentinel 流量管控全局异常处理器: 处理限流、熔断等流量层面的异常,并统一返回 Result 格式响应体
 *   ( 拦截发生在请求到达 Controller 方法之前，执行的层级是 Web Filter层 )
 */
@Component
public class SentinelExceptionHandler implements BlockExceptionHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
            // 429 Too Many Requests 标准HTTP状态码
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");

            // 复用项目统一的 Result 返回结构 (其实是调用项目的 Result 类来构造自身的Result对象返回,与项目中返回的的Result响应体保持一致)
            Result<?> result = Result.error(MessageConstant.SENTINEL_GLOBAL_ERROR);  // 注:目前的处理方式是把所有类型的Sentinel异常（限流、熔断等）都统一返回 "系统繁忙",后续再根据业务进行细分返回
            response.getWriter().write(JSON.toJSONString(result));
        }
}
