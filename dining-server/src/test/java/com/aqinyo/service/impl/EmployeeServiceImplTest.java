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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;           // 【注解】Mockito 的 JUnit5 扩展
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;            // JUnit 5 的断言方法
import static org.mockito.Mockito.*;                         // Mockito 核心静态方法（when/verify/times 等）


/*  启用 Mockito 注解支持 (每个测试类都要加)  */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    /*   创建 Mock假对象     (给下面"被测对象"的实例进行依赖注入的,而且下面的实例对象需要依赖注入什么,就 @Mock 几个假对象  / @Mock 是为 @InjectMocks 服务的)  */
    @Mock
    private EmployeeMapper employeeMapper;  // @Mock: 创建一个 EmployeeMapper "假对象" ,这个假Mapper并不会去连接数据库,所以方法默认返回 null --> 然后我们用 when(...).thenReturn(...) 来控制它的返回值
                                            // 并且针对的是:当前类（Service）所依赖的所有其他组件 (比如:Mapper/RabbitMQ/Redis等依赖),都会用 @Mock 假对象来替代

    /*   创建 "被测对象" 的实例      (被测对象原本需要依赖注入的所有类,都由上面 @Mock 的假对象来替代注入)  */
    @InjectMocks
    private EmployeeServiceImpl employeeService;  // @InjectMocks: 创建 EmployeeServiceImpl 实例,并自动把 EmployeeServiceImpl 要依赖注入的所有类, 全部由@Mock假对象来替代"被依赖注入"到EmployeeServiceImpl中
    private Employee testEmployee;


    /*  初始化 测试数据  */
    @BeforeEach    // 在每个@Test执行前,先执行一次 @BeforeEach 标注的方法  /  用途: 准备每个@Test都需要的公共数据,避免在每个@Test里重复写
    void setUp() {
        testEmployee = Employee.builder()       //这些数据是自行设定的,不需要和真实数据库中的数据对应,伪造但合法,
                .id(1L)                         //单元测试的核心原则是“隔离”,使用 @Mock 就不会真的去连接数据库执行 SQL,我们只测试 Service 层的业务逻辑,不依赖真实的数据库环境
                .username("admin")              //因此,只要构造的假数据能够满足 Service 内部的逻辑要求即可 (同样,也只需构造"对象属性拷贝+手动赋值"的属性即可,其余的公共字段可以自动填充)
                .name("管理员")
                .phone("13800138000")
                .sex("男")
                .idNumber("440101199001011234")
                .status(StatusConstant.ENABLE)
                .password(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes())) // 密码用 MD5 加密,模拟数据库中存储的密文
                .build();
    }



    // ================================= login() 方法 单元测试 =================================
    /*   作为首个单元测试内容,做了详细的笔记,可以随时温故(OrderServiceImplTest也可以辅助温故@Mock与@InjectMocks的关系)   */
    @Test
    @DisplayName("登录成功 - 用户名密码正确且账号启用") // 给测试方法起个名,控制台左侧的测试报告中会显示这个名字
    void login_success() {

        /*   Arrange（准备）   */
        // 【构造入参】--> 与上面的"初始化测试数据"一样,自行构造测试所需的入参数据DTO,不需要和真实数据库中的数据对应,越简单直接越好
        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("admin");  // 单元测试核心原则: “隔离、针对性”，对症构造入参
        employeeLoginDTO.setPassword("123456");  // 明文密码, service 内部会做 MD5 加密再比对

        // 【使用when().thenReturn()控制返回值】--> Mock是假Mapper对象,用来是隔离环境的,它什么都不做的,所以默认返回 null/0; 使用此 when().thenReturn() 控制其返回:测试方法所需要的测试数据值
        when(employeeMapper.getByUsername("admin")).thenReturn(testEmployee);

        /*   Act （执行）   */
        // 【执行被测方法】-->(上下文的准备都是围绕这个方法)获取返回结果后以推进后续验证
        Employee result = employeeService.login(employeeLoginDTO);

        /*   Assert（断言）  */
        // 【断言】--> 验证返回值是否符合预期  (它是单元测试的灵魂,是检验标准:决定了业务逻辑到底是对是错,直接关系到开发的功能是否可靠)
        assertNotNull(result);  // assertNotNull:返回值不应为 null ; 若预期返回值是 null,则应使用 assertNull
        assertEquals("admin", result.getUsername());    // 用户名应为 admin (确保返回结果result.getUsername()的用户名 = 预期值 "admin")
        assertEquals("管理员", result.getName());        // 姓名应为 管理员

        // 【使用verify(mock,times(n)).method()验证交互行为】--> 验证 Mock假对象 的交互行为（即验证某个方法是否被调用、调用次数及参数）
        verify(employeeMapper, times(1)).getByUsername("admin");  // 验证 Mock假对象(employeeMapper) 的交互行为(getByUsername方法) --> 恰好被调用了1次,且参数是"admin",测试才算通过
    }

    @Test
    @DisplayName("登录失败 - 账号不存在")    // 给测试方法起个名,控制台左侧的测试报告中会显示这个名字
    void login_accountNotFound() {
        // 入参
        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("abc");
        employeeLoginDTO.setPassword("123456");

        // 当查询不存在的用户名时,Mapper 返回 null（Mock 默认就返回 null,这里显式写出更清晰）
        when(employeeMapper.getByUsername("abc")).thenReturn(null);

        // 【使用assertThrows】--> 验证某段代码会抛出指定类型的异常。如果不抛或抛了别的类型，测试失败。
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> employeeService.login(employeeLoginDTO)); // 断言执行lambda表达式中的代码会抛出:AccountNotFoundException异常,如果没抛异常/抛了别的类型，测试失败(因为设定的就是抛自己定义好的异常-->才是预期)

        // 【断言】--> 验证 "异常信息exception.getMessage()" 是否= "我自定义的常量异常信息MessageConstant.ACCOUNT_NOT_FOUND"
        assertEquals(MessageConstant.ACCOUNT_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_passwordError() {

        EmployeeLoginDTO employeeLoginDTO = new EmployeeLoginDTO();
        employeeLoginDTO.setUsername("admin");
        employeeLoginDTO.setPassword("654321");  // 正确密码:123456

        when(employeeMapper.getByUsername("admin")).thenReturn(testEmployee);

        // 密码 MD5 不匹配 → 应抛出 PasswordErrorException
        PasswordErrorException exception = assertThrows(PasswordErrorException.class,
                () -> employeeService.login(employeeLoginDTO));

        // 【断言】--> 验证 "异常信息exception.getMessage()" 是否= "我自定义的常量异常信息MessageConstant.PASSWORD_ERROR"
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
        // 需要验证这三步都正确执行了

        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setUsername("newuser");
        employeeDTO.setName("新员工");
        employeeDTO.setPhone("13900139000");
        employeeDTO.setSex("女");
        employeeDTO.setIdNumber("440101199505051234");

        // 执行被测方法
        employeeService.add(employeeDTO);

        // 【关键 API】 --> ArgumentCaptor 参数捕获器
        // 为什么需要它？因为 add 方法内部 new 了一个 Employee 对象传给 mapper.insert(),我们拿不到这个内部创建的对象的引用，所以用 ArgumentCaptor "截获"它
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(captor.capture());  // 截获 insert 的参数

        Employee savedEmployee = captor.getValue();  // 拿到被截获的 Employee 对象

        // 【断言】
        // 验证 DTO → Entity 的属性拷贝是否正确
        assertEquals("newuser", savedEmployee.getUsername());
        assertEquals("新员工", savedEmployee.getName());
        assertEquals("13900139000", savedEmployee.getPhone());
        assertEquals("女", savedEmployee.getSex());
        assertEquals("440101199505051234", savedEmployee.getIdNumber());

        // 验证 Service 手动设置的默认值
        assertEquals(StatusConstant.ENABLE, savedEmployee.getStatus());  // 默认状态应为"启用"
        assertEquals(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()), savedEmployee.getPassword());  //默认密码应为MD5加密后的"123456"
    }




    // ================================= pageQuery() 方法 单元测试 =================================

    @Test
    @DisplayName("分页查询 - 正常返回数据")
    void pageQuery_success() {

        EmployeePageQueryDTO queryDTO = new EmployeePageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);

        // 构造一个模拟的分页结果
        Page<Employee> page = new Page<>(1, 10); // 1表示当前页码（第1页）, 10表示每页显示的记录数（每页10条）
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

        // Mock 默认返回 null，但显式写出来更清晰明白
        when(employeeMapper.getById(999L)).thenReturn(null);

        Employee result = employeeService.getById(999L);

        assertNull(result);
        verify(employeeMapper, times(1)).getById(999L);
    }




    // ================================= update() 方法 单元测试 =================================

    @Test
    @DisplayName("修改员工信息 - 正常流程")
    void update_success() {
        // 准备输入参数
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(1L);
        dto.setUsername("admin_updated");
        dto.setName("更新后的管理员");
        dto.setPhone("13700137000");
        dto.setSex("男");
        dto.setIdNumber("440101199001015678");

        // 执行被测方法
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
