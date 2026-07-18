package com.aqinyo.service;

import com.aqinyo.dto.EmployeeDTO;
import com.aqinyo.dto.EmployeeLoginDTO;
import com.aqinyo.dto.EmployeePageQueryDTO;
import com.aqinyo.entity.Employee;
import com.aqinyo.result.PageResult;

public interface EmployeeService {

    // 登录
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    // 新增
    void add(EmployeeDTO employeeDTO);

    // 分页查询
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    // 启用禁用
    void startOrStop(int status, Long id);

    // 查询 (根据id)
    Employee getById(long id);

    // 修改
    void update(EmployeeDTO employeeDTO);
}
