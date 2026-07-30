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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/*  配置类: 注册web层相关组件  */

@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;  //依赖注入admin端jwt令牌校验的拦截器
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;    //依赖注入user端jwt令牌校验的拦截器

    /*  设置静态资源映射	 (对于接口文档:静态放行)  (对于JWT:静态放行+ 下面动态拦截)   */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射(静态放行)...");

        // 映射 /doc.html (Knife4j/Swagger) 的文档页面
        registry.addResourceHandler("/doc.html")    //放行 Knife4j 的前端页面文件,不被 JWT 拦截器挡住 (注意:接口文档页面上展示的接口数据是项目运行时动态生成的噢！)
                .addResourceLocations("classpath:/META-INF/resources/");     // 还涉及:接口文档页面渲染所需的其他静态资源（如 css/js 等，通常还需放行 /** 到 META-INF/resources，此处仅配置了核心入口）

        // 映射 /webjars下所有的前端静态资源  (如Swagger UI渲染所需的JS/CSS等)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");     // Spring Boot 会自动将这些 jar 中的静态资源映射到 classpath:/META-INF/resources/webjars/ 目录下
    }


    /*  注册自定义JWT拦截器     (拦截器+注册拦截器 搭配才有用)    */      // 注册拦截器就相当于 --> 给拦截器分配任务(告诉它该干什么？)
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义JWT拦截器(动态拦截)...");

        // 注册 admin端JWT拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")                   // 添加 以/admin开头的所有请求-->执行拦截"任务"(即如果绕过登录不获取token,则请求都为无效,然后拦截器不放行)
                .excludePathPatterns("/admin/employee/login");  // 排除 /admin/employee/login请求的拦截"任务"
                                                                /* 总结: 这个拦截器任务是 --> 做JWT校验拦截,没有token就不放行+报401错;只有登录这个请求是放行的,因为要放你登录拿token嘛 */
        // 注册 user端JWT拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status")
                .excludePathPatterns("/user/dish/list");        // 因为这里需要做压测,所以先放行一下这个接口(根据分类id查询菜品)
    }


    /*  通过 knife4j框架 自动生成 "admin端" 接口文档 (格式可参考复用)  */
    @Bean   // B端 admin的
    public Docket adminDocket() {   /*  Docket 是 springfox 的核心配置对象,代表 "一组接口文档"  */
        log.info("开始通过knife4j框架自动生成/controller/admin的接口文档...");

        ApiInfo apiInfo123 = new ApiInfoBuilder()  // ApiInfoBuilder设置接口文档的 "标题、作者、版本、描述" 等元信息
                .title("餐饮服务项目接口文档")
                .contact(new Contact("Aqinyo", "https://github.com/Aqinyo", "3186538497@qq.com"))
                .version("1.0")
                .description("通过 knife4j框架 来自动生成: 项目admin端的接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2) //指定 使用 Swagger2 规范
                .groupName("管理端接口")         //设置 接口文档分组名  (可以有多个Docket，所以这里给它起个名字)
                .apiInfo(apiInfo123)           //设置 接口文档元信息  (即: 将上面 ApiInfo对象 装进docket中)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aqinyo.controller.admin"))   //设置 生成接口需要扫描的包
                .paths(PathSelectors.any())    //设置 该包下所有路径都纳入接口文档
                .build();

        return docket;
    }

    /*  通过 knife4j框架 自动生成 "user端" 接口文档 (格式可参考复用)  */
    @Bean   // C端 user的
    public Docket userDocket() {
        log.info("开始通过knife4j框架自动生成/controller/user的接口文档...");

        ApiInfo apiInfo321 = new ApiInfoBuilder()
                .title("餐饮服务项目接口文档")
                .contact(new Contact("Aqinyo", "https://github.com/Aqinyo", "3186538497@qq.com"))
                .version("1.0")
                .description("通过 knife4j框架 来自动生成: 项目user端的接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo321)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aqinyo.controller.user"))
                .paths(PathSelectors.any())
                .build();

        return docket;
    }


    /*  扩展Spring MVC框架的消息转化器  */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器......");

        //创建一个消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //为消息转换器设置一个对象转换器，对象转换器可以将java对象转换为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转换器加入容器中
        converters.add(0, converter);

    }

}
