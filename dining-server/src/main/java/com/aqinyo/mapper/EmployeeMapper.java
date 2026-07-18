package com.aqinyo.mapper;

import com.github.pagehelper.Page;
import com.aqinyo.annotation.AutoFill;
import com.aqinyo.dto.EmployeePageQueryDTO;
import com.aqinyo.entity.Employee;
import com.aqinyo.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/*   员工管理 Mapper层   */

@Mapper
public interface EmployeeMapper {

    // 查询员工 (根据用户名)
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    // 插入员工 (单表插入,简单SQL用注解)
    @Insert("insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) " +
            "VALUES (#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

    // 分页查询
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);     //带小鸟图标的都是 --> xml配置方式

    // 更新
    @AutoFill(value = OperationType.UPDATE)    // update和insert类型的SQL都使用自定义注解@AutoFill做标记,拦截住统一做AOP增强
    void update(Employee employee);

    // 查询员工 (根据id)
    @Select("select * from employee where id = #{id}")
    Employee getById(long id);

}
