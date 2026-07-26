package com.aqinyo.service.impl;

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
import com.github.pagehelper.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;           // 【注解】启用 Mockito 扩展
import org.mockito.ArgumentCaptor;                           // 【注解】参数捕获器
import org.mockito.InjectMocks;                              // 【注解】创建被测类实例并注入 Mock 依赖
import org.mockito.Mock;                                     // 【注解】创建 Mock 假对象
import org.mockito.junit.jupiter.MockitoExtension;           // 【注解】Mockito 的 JUnit5 扩展
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;            // JUnit 5 的断言方法
import static org.mockito.Mockito.*;                         // Mockito 核心静态方法（when/verify/times 等）


/*  启用 Mockito 注解支持 (每个测试类都要加)  */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    /*   创建 Mock 假对象   */
    @Mock
    private EmployeeMapper employeeMapper;  // @Mock 创建一个 EmployeeMapper "假对象" ,这个假Mapper不会连接数据库，方法默认返回 null --> 然后我们用 when(...).thenReturn(...) 来控制它的返回值


    /*   创建 "被测对象" 的实例   */
    @InjectMocks
    private EmployeeServiceImpl employeeService;  // @InjectMocks 创建 EmployeeServiceImpl 实例,并自动把上面的 @Mock 假对象注入到它的 employeeMapper 字段中。等价于：new EmployeeServiceImpl()，但其内部的 employeeMapper 是我们的假对象
    private Employee testEmployee;  // 测试用的员工实体对象（每个测试方法执行前都会重新创建）


    /*  初始化 测试数据  */
    @BeforeEach    // 在每个@Test执行前,先执行一次 @BeforeEach 标注的方法  /  用途: 准备每个@Test都需要的公共数据,避免在每个@Test里重复写
    void setUp() {
        testEmployee = Employee.builder()
                .id(1L)
                .username("admin")
                .name("管理员")
                .phone("13800138000")
                .sex("男")
                .idNumber("440101199001011234")
                .status(StatusConstant.ENABLE)
                // 密码用 MD5 加密，模拟数据库中存储的密文
                .password(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()))
                .build();
    }



    /*   作为首个单元测试内容,做了详细的笔记,可以随时温故   */
    // ================================= login() 方法 单元测试 =================================

    @Test
    @DisplayName("登录成功 - 用户名密码正确且账号启用") // 给测试方法起个名,测试报告中会显示这个名字
    void login_success() {

        // 准备输入参数
        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("admin");  // 单元测试的核心原则是: “隔离、针对性”,只关注被测方法本身的逻辑。因此不需要考虑解耦,直接“对症”构造该方法需要的入参即可，越简单直接越好
        employeeLoginDTO.setPassword("123456");  // 明文密码,service 内部会做 MD5 加密再比对

        // 【关键API】 因为这个假Mapper是隔离环境的,不会连接数据库,方法默认返回 null --> 所以使用 when().thenReturn() 来控制返回值
        when(employeeMapper.getByUsername("admin")).thenReturn(testEmployee);

        /*  执行被测方法  (上下文的准备都是围绕这个方法)  */
        Employee result = employeeService.login(employeeLoginDTO);

        // 【断言】验证返回值
        assertNotNull(result);  // assertNotNull 即返回值不应为 null
        assertEquals("admin", result.getUsername());    // 用户名应为 admin
        assertEquals("管理员", result.getName());        // 姓名应为 管理员

        // 【关键API】 verify(mock, times(n)).method()
        // verify() 是 Mockito 框架提供的核心API,专门用于验证 Mock假对象 的交互行为（即验证某个方法是否被调用、调用次数及参数）
        verify(employeeMapper, times(1)).getByUsername("admin");  // 验证 Mock假对象(employeeMapper) 的交互行为(getByUsername方法)-->恰好被调用了1次,且参数是"admin"
    }

    @Test
    @DisplayName("登录失败 - 账号不存在")    // 给测试方法起个名,测试报告中会显示这个名字
    void login_accountNotFound() {

        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("notexist");
        employeeLoginDTO.setPassword("123456");

        // 当查询不存在的用户名时，Mapper 返回 null（Mock 默认就返回 null，这里显式写出更清晰）
        when(employeeMapper.getByUsername("notexist")).thenReturn(null);

        // 【关键 API】assertThrows
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> employeeService.login(employeeLoginDTO)); // 断言执行lambda表达式中的代码会抛出:AccountNotFoundException异常,如果没抛异常/抛了别的类型，测试失败(因为设定的就是抛自己定义好的异常-->才是预期)

        // 验证 异常信息 是否与 我自定义的常量 一致
        assertEquals(MessageConstant.ACCOUNT_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_passwordError() {

        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("admin");
        employeeLoginDTO.setPassword("wrongpassword");  // 错误密码

        when(employeeMapper.getByUsername("admin")).thenReturn(testEmployee);

        // 密码 MD5 不匹配 → 应抛出 PasswordErrorException
        PasswordErrorException exception = assertThrows(PasswordErrorException.class,
                () -> employeeService.login(employeeLoginDTO));
        assertEquals(MessageConstant.PASSWORD_ERROR, exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 账号被锁定")
    void login_accountLocked() {

        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("admin");
        employeeLoginDTO.setPassword("123456");  // 密码正确

        // 构造一个"被锁定"的员工（status = DISABLE）
        Employee lockedEmployee = Employee.builder()
                .id(2L)
                .username("admin")
                .password(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()))
                .status(StatusConstant.DISABLE)   // ← 关键：账号被禁用
                .build();

        when(employeeMapper.getByUsername("admin")).thenReturn(lockedEmployee);

        // 密码正确但账号被锁定 → 应抛出 AccountLockedException
        AccountLockedException exception = assertThrows(AccountLockedException.class,
                () -> employeeService.login(employeeLoginDTO));
        assertEquals(MessageConstant.ACCOUNT_LOCKED, exception.getMessage());
    }




    // ================================= add() 方法 单元测试 =================================

    @Test
    @DisplayName("新增员工 - 正常流程（验证属性拷贝和默认值设置）")
    void add_success() {
        // 【测试思路】add 方法内部做了三件事：
        // 1. BeanUtils.copyProperties 把 DTO 属性拷贝到 Entity
        // 2. 手动设置 status = ENABLE, password = MD5(123456)
        // 3. 调用 mapper.insert(employee)
        // 我们需要验证这三步都正确执行了

        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setUsername("newuser");
        employeeDTO.setName("新员工");
        employeeDTO.setPhone("13900139000");
        employeeDTO.setSex("女");
        employeeDTO.setIdNumber("440101199505051234");

        // 执行被测方法
        employeeService.add(employeeDTO);

        // 【关键 API】ArgumentCaptor -- 参数捕获器
        // 为什么需要它？因为 add 方法内部 new 了一个 Employee 对象传给 mapper.insert()，
        // 我们拿不到这个内部创建的对象的引用，所以用 ArgumentCaptor "截获"它
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(captor.capture());  // 截获 insert 的参数

        Employee savedEmployee = captor.getValue();  // 拿到被截获的 Employee 对象

        // 验证 DTO → Entity 的属性拷贝是否正确
        assertEquals("newuser", savedEmployee.getUsername());
        assertEquals("新员工", savedEmployee.getName());
        assertEquals("13900139000", savedEmployee.getPhone());
        assertEquals("女", savedEmployee.getSex());
        assertEquals("440101199505051234", savedEmployee.getIdNumber());

        // 验证 Service 手动设置的默认值
        assertEquals(StatusConstant.ENABLE, savedEmployee.getStatus());   // 默认状态应为"启用"
        assertEquals(
                DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()),
                savedEmployee.getPassword()   // 默认密码应为 MD5 加密后的 "123456"
        );
    }




    // ================================= pageQuery() 方法 单元测试 =================================

    @Test
    @DisplayName("分页查询 - 正常返回数据")
    void pageQuery_success() {

        EmployeePageQueryDTO queryDTO = new EmployeePageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        // 构造一个模拟的分页结果
        Page<Employee> page = new Page<>(1, 10);
        List<Employee> employeeList = new ArrayList<>();
        employeeList.add(testEmployee);
        page.addAll(employeeList);
        page.setTotal(1);

        when(employeeMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult result = employeeService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(employeeMapper, times(1)).pageQuery(queryDTO);
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    void pageQuery_emptyResult() {

        EmployeePageQueryDTO queryDTO = new EmployeePageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        Page<Employee> page = new Page<>(1, 10);
        page.setTotal(0);

        when(employeeMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult result = employeeService.pageQuery(queryDTO);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }




    // ================================= startOrStop() 方法 单元测试 =================================

    @Test
    @DisplayName("启用员工账号")
    void startOrStop_enable() {

        employeeService.startOrStop(StatusConstant.ENABLE, 1L);

        // 用 ArgumentCaptor 截获传给 mapper.update 的 Employee 对象
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(captor.capture());

        Employee captured = captor.getValue();
        assertEquals(StatusConstant.ENABLE, captured.getStatus());
        assertEquals(1L, captured.getId());
    }

    @Test
    @DisplayName("禁用员工账号")
    void startOrStop_disable() {

        employeeService.startOrStop(StatusConstant.DISABLE, 1L);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(captor.capture());

        Employee captured = captor.getValue();
        assertEquals(StatusConstant.DISABLE, captured.getStatus());
        assertEquals(1L, captured.getId());
    }




    // ================================= getById() 方法 单元测试 =================================

    @Test
    @DisplayName("根据ID查询员工 - 员工存在")
    void getById_exists() {
        when(employeeMapper.getById(1L)).thenReturn(testEmployee);

        Employee result = employeeService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("admin", result.getUsername());
        verify(employeeMapper, times(1)).getById(1L);
    }

    @Test
    @DisplayName("根据ID查询员工 - 员工不存在返回null")
    void getById_notExists() {
        // Mock 默认返回 null，但显式写出更清晰
        when(employeeMapper.getById(999L)).thenReturn(null);

        Employee result = employeeService.getById(999L);

        assertNull(result);
        verify(employeeMapper, times(1)).getById(999L);
    }




    // ================================= update() 方法 单元测试 =================================

    @Test
    @DisplayName("修改员工信息 - 正常流程")
    void update_success() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(1L);
        dto.setUsername("admin_updated");
        dto.setName("更新后的管理员");
        dto.setPhone("13700137000");
        dto.setSex("男");
        dto.setIdNumber("440101199001015678");

        employeeService.update(dto);

        // 截获传给 mapper.update 的参数，验证属性拷贝是否正确
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(captor.capture());

        Employee captured = captor.getValue();
        assertEquals(1L, captured.getId());
        assertEquals("admin_updated", captured.getUsername());
        assertEquals("更新后的管理员", captured.getName());
        assertEquals("13700137000", captured.getPhone());
    }

}
