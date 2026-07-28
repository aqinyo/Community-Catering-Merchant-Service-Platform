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
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/*  配置类: 注册web层相关组件  */

@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;//admin的jwt令牌校验的拦截器
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;//user的jwt令牌校验的拦截器


    /*  设置静态资源映射    (静态放行 + 下面动态拦截)  */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射(静态放行)...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


    /*  注册自定义拦截器     (拦截器+注册拦截器 搭配才有用)    */      // 注册拦截器就相当于 --> 给拦截器分配任务(告诉它该干什么？)
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器(动态拦截)...");

        registry.addInterceptor(jwtTokenAdminInterceptor)       // 注册 拦截器
                .addPathPatterns("/admin/**")                   // 添加 以/admin开头的所有请求-->执行拦截"任务"(即如果绕过登录不获取token,则请求都为无效,然后拦截器不放行)
                .excludePathPatterns("/admin/employee/login");  // 排除 /admin/employee/login请求的拦截"任务"
                                                            /* 总结: 这个拦截器任务是 --> 做JWT校验拦截,没有token就不放行+报401错;只有登录这个请求是放行的,因为要放你登录拿token嘛 */
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status")
                .excludePathPatterns("/user/dish/list");        // 因为这里需要做压测,所以先放行一下这个接口(根据分类id查询菜品)
    }


    /*  通过 knife4j框架 自动生成接口文档 (格式可参考复用)  */
    @Bean   // B端 admin的
    public Docket adminDocket() {   /*  Docket 是 springfox 的核心配置对象,代表 "一组接口文档"  */

        ApiInfo apiInfo123 = new ApiInfoBuilder()  // ApiInfoBuilder 设置文档的 "标题、版本、描述" 等元信息
                .title("商城外卖项目接口文档")
                .version("2.0")
                .description("通过knife4j框架来生成-->商城外卖项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2) //指定使用 Swagger 2 规范
                .groupName("管理端接口") //设置 文档分组名 (可以有多个Docket，所以这里给它起个名字)
                .apiInfo(apiInfo123)    //设置 文档元信息(即把上面 ApiInfo的对象 设置到docket中)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aqinyo.controller.admin"))// 设置 只扫描这个包下的接口
                .paths(PathSelectors.any())// 设置 该包下所有路径都纳入文档
                .build();

        return docket;
    }

    @Bean   // C端 user的
    public Docket userDocket() {

        ApiInfo apiInfo321 = new ApiInfoBuilder()
                .title("商城外卖项目接口文档")
                .version("2.0")
                .description("通过knife4j框架来生成-->商城外卖项目接口文档")
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
