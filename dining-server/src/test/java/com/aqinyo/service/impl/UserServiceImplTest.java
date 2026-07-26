package com.aqinyo.service.impl;

import com.aqinyo.constant.MessageConstant;
import com.aqinyo.dto.UserLoginDTO;
import com.aqinyo.entity.User;
import com.aqinyo.exception.LoginFailedException;
import com.aqinyo.mapper.UserMapper;
import com.aqinyo.properties.JwtProperties;
import com.aqinyo.properties.WeChatProperties;
import com.aqinyo.utils.HttpClientUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private WeChatProperties weChatProperties;

    @InjectMocks
    private UserServiceImpl userService;

    // ==================== wxlogin 方法测试 ====================

    @Test
    @DisplayName("微信登录 - 老用户直接返回")
    void wxlogin_existingUser() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setCode("test_code");

        // Mock 微信配置属性
        when(weChatProperties.getAppid()).thenReturn("test_appid");
        when(weChatProperties.getSecret()).thenReturn("test_secret");

        // Mock HttpClientUtil.doGet 静态方法，返回模拟的微信响应
        // 注意：因为 HttpClientUtil.doGet 是静态方法，需要使用 mockStatic
        try (MockedStatic<HttpClientUtil> mockedHttp = mockStatic(HttpClientUtil.class)) {
            mockedHttp.when(() -> HttpClientUtil.doGet(anyString(), anyMap()))
                    .thenReturn("{\"openid\":\"test_openid_123\"}");

            // Mock 数据库查询：老用户已存在
            User existingUser = User.builder()
                    .id(1L)
                    .openid("test_openid_123")
                    .name("张三")
                    .createTime(LocalDateTime.now().minusDays(30))
                    .build();
            when(userMapper.getByOpenid("test_openid_123")).thenReturn(existingUser);

            User result = userService.wxlogin(dto);

            // 验证返回的是已存在的用户
            assertNotNull(result);
            assertEquals("test_openid_123", result.getOpenid());
            assertEquals("张三", result.getName());

            // 老用户不应触发 insert
            verify(userMapper, never()).insert(any());
            verify(userMapper, times(1)).getByOpenid("test_openid_123");
        }
    }

    @Test
    @DisplayName("微信登录 - 新用户自动注册")
    void wxlogin_newUser() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setCode("test_code");

        when(weChatProperties.getAppid()).thenReturn("test_appid");
        when(weChatProperties.getSecret()).thenReturn("test_secret");

        try (MockedStatic<HttpClientUtil> mockedHttp = mockStatic(HttpClientUtil.class)) {
            mockedHttp.when(() -> HttpClientUtil.doGet(anyString(), anyMap()))
                    .thenReturn("{\"openid\":\"new_openid_456\"}");

            // Mock 数据库查询：新用户不存在
            when(userMapper.getByOpenid("new_openid_456")).thenReturn(null);

            User result = userService.wxlogin(dto);

            // 验证新用户被自动注册
            assertNotNull(result);
            assertEquals("new_openid_456", result.getOpenid());

            // 验证 insert 被调用（自动注册）
            verify(userMapper, times(1)).insert(any(User.class));
        }
    }

    @Test
    @DisplayName("微信登录 - openid为空，抛出异常")
    void wxlogin_openidNull() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setCode("invalid_code");

        when(weChatProperties.getAppid()).thenReturn("test_appid");
        when(weChatProperties.getSecret()).thenReturn("test_secret");

        try (MockedStatic<HttpClientUtil> mockedHttp = mockStatic(HttpClientUtil.class)) {
            // 模拟微信返回空 openid（code 无效时的响应）
            mockedHttp.when(() -> HttpClientUtil.doGet(anyString(), anyMap()))
                    .thenReturn("{}");  // 无 openid 字段，getString 返回 null

            LoginFailedException exception = assertThrows(LoginFailedException.class,
                    () -> userService.wxlogin(dto));
            assertEquals(MessageConstant.LOGIN_FAILED, exception.getMessage());
        }
    }
}
