package com.aqinyo.config;

import com.aqinyo.interceptor.JwtTokenAdminInterceptor;
import com.aqinyo.interceptor.JwtTokenUserInterceptor;
import com.aqinyo.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/*  配置类: 注册web层相关组件  */

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;  //依赖注入admin端jwt令牌校验的拦截器
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;    //依赖注入user端jwt令牌校验的拦截器


    /*  设置静态资源映射	 (对于接口文档:静态放行)  (对于JWT:静态放行+ 下面动态拦截)   */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射(静态放行)...");

        // Knife4j 的静态资源映射
        registry.addResourceHandler("/doc.html")    //放行 Knife4j 的前端页面文件,不被 JWT 拦截器挡住 (注意:接口文档页面上展示的接口数据是项目运行时动态生成的噢！)
                .addResourceLocations("classpath:/META-INF/resources/");     // 还涉及:接口文档页面渲染所需的其他静态资源（如 css/js 等，通常还需放行 /** 到 META-INF/resources，此处仅配置了核心入口）

        // 映射 /webjars下所有的前端静态资源  (如Swagger UI渲染所需的JS/CSS等)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");     // Spring Boot 会自动将这些 jar 中的静态资源映射到 classpath:/META-INF/resources/webjars/ 目录下
    }



    /*  注册自定义JWT拦截器     (拦截器+注册拦截器 搭配才有用)    */      // 注册拦截器就相当于 --> 给拦截器分配任务(告诉它该干什么？)
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义JWT拦截器(动态拦截)...");

        // 注册 admin端JWT拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")                   // 添加 以/admin开头的所有请求-->执行拦截"任务"(即如果绕过登录不获取token,则请求都为无效,然后拦截器不放行)
                .excludePathPatterns("/admin/employee/login");  // 排除 /admin/employee/login请求的拦截"任务"

        // 注册 user端JWT拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status")
                .excludePathPatterns("/user/dish/list");       // 因为这里需要做压测,所以先放行一下这个接口(根据分类id查询菜品)
    }



    /*  扩展Spring MVC框架的消息转化器  */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器......");

        //创建一个消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //为消息转换器设置一个对象转换器，对象转换器可以将java对象转换为json数据
        converter.setObjectMapper(new JacksonObjectMapper());

        // 将自己的消息转换器加入容器中,且只add,不要指定index=0的优先级设定,0是指第一个位置,放到列表尾部,不影响框架内部接口的正常运行
        // converters.add(0, converter);
        /* 指定index=0后,导致自定义过滤器干扰了响应输出,返回的不是标准JSON,而是被gzip压缩后的二进制流,浏览器没有自动解压,直接把压缩字节当成文本渲染,前端knife4j解析JSON失败,因此我的接口文档无法在网页打开) */
        converters.add(converter);

    }




    /*  通过 SpringDoc + Knife4j 自动生成接口文档的配置   (格式可参考复用)  */
    @Bean
    public OpenAPI customOpenAPI() {    // OpenAPI是全局配置: 只不过这里把要定义的接口文档基本信息,抽取到PublicInfo()方法中了
        return new OpenAPI().info(PublicInfo());
    }

    private Info PublicInfo() {  // B/C端的公共文档信息
        return new Info()
                .title("社区餐饮服务项目接口文档")
                .contact(new Contact()       // 负责人信息
                        .name("Aqinyo")
                        .url("https://github.com/Aqinyo")
                        .email("3186538497@qq.com"))
                .version("3.0")
                .description("通过 SpringDoc 1.7.0  +  Knife4j 3.0.3 来自动生成接口文档");
    }

    /*  admin端 接口文档   */
    @Bean
    public GroupedOpenApi adminApi() {      // GroupedOpenApi是分组配置
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.aqinyo.controller.admin")
                .build();
    }
    /*  user端 接口文档   */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .packagesToScan("com.aqinyo.controller.user")
                .build();
    }

}
