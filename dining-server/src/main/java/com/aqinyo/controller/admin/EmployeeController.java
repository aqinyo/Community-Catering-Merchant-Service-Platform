package com.aqinyo.controller.admin;

import com.aqinyo.constant.JwtClaimsConstant;
import com.aqinyo.dto.EmployeeDTO;
import com.aqinyo.dto.EmployeeLoginDTO;
import com.aqinyo.dto.EmployeePageQueryDTO;
import com.aqinyo.entity.Employee;
import com.aqinyo.properties.JwtProperties;
import com.aqinyo.result.PageResult;
import com.aqinyo.result.Result;
import com.aqinyo.service.EmployeeService;
import com.aqinyo.utils.JwtUtil;
import com.aqinyo.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/*    员工管理 Controller层    */

@RestController     // @Controller + @ResponseBody
@RequestMapping("/admin/employee")     //请求路径的前缀
@Slf4j
@Api(tags = "admin端-员工相关接口")  /* 类级别: 描述接口分组   (@Api和@ApiModel等都是Swagger的注解,是Swagger扫描这些注解时,读取的 "接口文档" 素材)  */
public class EmployeeController {                                                // 在我项目中采取的是 "先编码,后自动生成文档" 的接口文档设计模式

    @Autowired
    private EmployeeService employeeService;    // 多态写法-->依赖注入的是service接口实现类serviceImpl的对象
    @Autowired
    private JwtProperties jwtProperties;    // 登录涉及JWT校验,所以依赖注入属性配置类(需要读取配置文件的jwt的值)

    /*  登录  */
    @PostMapping("/login")
    @ApiOperation("员工登录")   /*  方法级别: 描述接口分组下的单个接口  */
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后,为员工 生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        // 把要返回的id、openid、token封装到VO类中返回前端 (然后"员工"也能有token进入程序做操作了)
        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)   //设置token: 是上面自己写的JwtUtil工具类中的createJWT方法,创建出来的token
                .build();

        return Result.success(employeeLoginVO); //返回VO类 (里面封装的是id、openid、token这三)
    }

    /*  退出  */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout() {
        return Result.success();
    }

    /*  新增员工  */
    @PostMapping
    @ApiOperation("新增员工")
    public Result<String> add(@RequestBody EmployeeDTO employeeDTO){    //发送json数据则都用@RequestBody给形参加上,且controller的方法类型和返回值基本都是Result<T>
        log.info("新增员工：{}", employeeDTO);
        employeeService.add(employeeDTO);   /* 然后调用service层的方法进行业务操作 */
        return Result.success();    //返回无参的,因为没有需要返回的VO类数据
    }

    /*  分页查询  */
    @GetMapping("/page")     //REST风格的请求路径 --> 里面的请求路径也是,根据接口文档设计的来加入即可,不是自己定义的
    @ApiOperation("员工分页查询")     //API的描述
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){  //这里不是传json数据了所以没加@RequestBody(分页查询都不加)
        log.info("员工分页查询：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);    //封装成 PageResult对象(封装分页查询结果)
        return Result.success(pageResult);
    }

    /*  启用/禁用 员工账号  */
    @PostMapping("/status/{status}")    //REST风格的请求路径 --> 里面的请求路径也是,根据接口文档设计的来加入即可,不是自己定义的
    @ApiOperation("启用或禁用员工账号")      //API的描述
    public Result<String> startOrStop(@PathVariable int status, Long id){   //这里是属于路径参数,所以加@PathVariable,而且要注意这里的: 形参名 = 接口文档的参数名
        log.info("启用或禁用员工账号：{}，{}", status, id);
        employeeService.startOrStop(status, id);
        return Result.success();    // 返回Result结果集类的无参success方法 (这里不用返回数据,所以调用的是无参)
    }

    /*  查询员工信息 (根据id)  */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询员工信息")
    public Result<Employee> getById(@PathVariable long id){
        log.info("回显员工信息：{}", id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);    // 这里则是返回Result结果集类的有参success方法了 (因为携带了实体类employee要返回给前端)
    }              /*  形参这偷懒传了实体类employee,规范的应是-->实体类转换为VO类(对象属性拷贝)然后再由Result的data携带给前端 ; (转换为VO类就能隐藏密码、身份证等核心信息,而直接返回实体类会全暴露给前端)*/

    /*  修改员工信息      (这里修改是"先查后改",涉及到两个接口的)  */
    @PutMapping
    @ApiOperation("修改员工信息")
    public Result<String> update(@RequestBody EmployeeDTO employeeDTO){
        log.info("修改员工信息：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

}
