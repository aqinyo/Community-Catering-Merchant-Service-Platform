package com.aqinyo.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.aqinyo.constant.MessageConstant;
import com.aqinyo.constant.PasswordConstant;
import com.aqinyo.constant.StatusConstant;
import com.aqinyo.dto.EmployeeDTO;
import com.aqinyo.dto.EmployeeLoginDTO;
import com.aqinyo.dto.EmployeePageQueryDTO;
import com.aqinyo.entity.Employee;
import com.aqinyo.exception.AccountLockedException;
import com.aqinyo.exception.AccountNotFoundException;
import com.aqinyo.exception.PasswordErrorException;
import com.aqinyo.mapper.EmployeeMapper;
import com.aqinyo.result.PageResult;
import com.aqinyo.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/*   员工管理 Service层    */

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;  // 多态写法-->依赖注入的是mapper接口实现类的对象

    /*  登录  */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在 --> 抛出"账号不存在"异常 (引用common模块的自定义常量类)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        /*  对前端传过来的明文密码进行 MD5 加密,然后再与数据库的密文密码进行比对   (现在仅仅为了验证是否能前后端联调才暂时注释而已,数据库加上密文后可放开)  */
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (employee.getStatus() == StatusConstant.DISABLE) {
            // 账号被锁定 --> 抛出"账号被锁定"异常 (引用common模块的自定义常量类)
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);//
        }

        //3、返回实体对象
        return employee;
    }

    /*   新增员工   */
    @Override
    public void add(EmployeeDTO employeeDTO) {  /* DTO类用于接收前端数据,现在进入DAO层到数据库,则要把 DTO类 --> Entity实体类 (转换实现方法: 对象属性拷贝 + 手动赋值剩余属性) */
        Employee employee = new Employee();                         /* 实体类转换为VO类也是如此: 通过"对象属性拷贝"转换为VO类,由Result的data携带给前端 */
        // 使用BeanUtils工具类 --> 对象属性拷贝 (见名知意)
        BeanUtils.copyProperties(employeeDTO, employee);   // 把DTO类属性拷贝到Entity类中(前形参--copy为-->后形参),前提是要拷贝的属性是两个类都有的
                                                           // Entity中的属性比DTO多的,可以说是要copy的属性是被entity包含的,但是Entity类中也有DTO类没有的属性,这时候就得自己收到设设置了

        /* 往下就是 --> Entity类有的属性而DTO类没有的属性  要自己手动设置了 */
        // 设置账号状态
        employee.setStatus(StatusConstant.ENABLE);  //1/0:可用/不可用; 这里不写死,否则是硬编码; 用常量类中的替代,常量类在"子模块common"中
        // 设置密码,默认123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes())); // 使用DigestUtils进行加密设置密码(密码也是在通用模块中设置好了,这里引用常量即可)

        /*   可以通过AOP切面类-->实现公共字段自动赋值,无需手动赋值 (指四个公共的部分: CreateTime，UpdateTime、CreateUser的ID、UpdateUser的ID)   */
        //LocalDateTime time = LocalDateTime.now();
        //设置当前记录的创建时间和修改时间         TODO(仅学习加上的,TODO作用-->可查看自己之前要后期修改而做的标记)
        //employee.setCreateTime(time);
        //employee.setUpdateTime(time);

        //设置当前记录创建人id 和 修改人id
        //employee.setCreateUser(BaseContext.getCurrentId());
        //employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);    //调用mapper的插入方法,把转换成实体类的前端数据employee 插入到数据库中
    }


    /*   分页查询   */
    @Override             // 实现的是接口什么方法,可以点左边的“接口”按钮 --> 到接口查看,那边都有注释
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());   // 分页查询都会用到 PageHelper(分页插件)
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);   //用到了employeeMapper接口的pageQuery分页查询方法
        long total = page.getTotal();
        List<Employee> employeeList = page.getResult();
        return new PageResult(total, employeeList);
    }


    /*   启用禁用 员工账号  */
    @Override
    public void startOrStop(int status, Long id) {

        //根据Employee类中的 @Builder注解 进行实现类的构造 --> 在创建了employee实体类对象的同时,还能设置了想要设置的status和id属性值
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();

        employeeMapper.update(employee);
    }


    /*   根据id查询 员工信息   */
    @Override
    public Employee getById(long id) {
        return employeeMapper.getById(id);
    }


    /*   修改 员工信息   */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        BeanUtils.copyProperties(employeeDTO, employee);    //对象属性拷贝,将前端的数据封装好的DTO类--转换-->实体类

        /*   可以通过AOP切面类-->实现公共字段自动赋值,无需手动赋值 (指四个公共的部分: CreateTime，UpdateTime、CreateUser的ID、UpdateUser的ID)   */
        //employee.setUpdateUser(BaseContext.getCurrentId());
        //employee.setUpdateTime(LocalDateTime.now());

        employeeMapper.update(employee);
    }

}
