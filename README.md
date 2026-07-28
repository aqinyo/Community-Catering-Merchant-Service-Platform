# Dining_service_platform（社区餐饮商户服务平台）

#### 介绍
- 该项目是“社区餐饮商户服务平台”，是基于SpringBoot的后端服务平台，是单体项目，包含我的代码和记录的笔记知识等
- 开发时间:2026.03 - 2026.06
- 项目性质:个人后端

#### 技术栈
- 核心框架: Spring Boot + Spring MVC
- 数据库框架: MyBatis + Druid连接池 + PageHelper分页插件
- 缓存: Redis
- 消息队列: RabbitMQ
- 安全认证: JWT
- 云存储: 阿里云 OSS
- 版本控制: Git
- 容器: Docker
- 环境配置: 多环境yml（dev、test、pro）
- 测试与规范: Junit5 + Mockito、Apifox
- 接口文档: Knife4j + Swagger2

#### 运行环境
- 操作系统: Windows 10
- 开发工具: IntelliJ IDEA（Amazon Corretto 1.8.0_452）
- 数据库: MySQL 8.0 （本地）
- 中间件运行操作系统: Linux（CentOS 7）
- 缓存: Redis 6.2.6
- 消息队列: RabbitMQ 3.9+
- 容器环境: Docker 26.1.4
    
#### 软件架构
单体架构（controller - service - mapper 三层架构）

#### 项目结构
分为三个子模块: dining-common（公共通用模块）、dining-model（DTO/实体类/VO的数据模型模块）、dining-server（具体业务模块）

#### 亮点与细节
1. 规范开发、JWT鉴权、DTO/VO/实体数据隔离、mapper自动代理、RESTful接口规范、 统一响应格式:Result封装
2. 引入 Redis 优化热点数据缓存
3. RabbitMQ 异步解耦与延迟消息
4. 集成阿里云 OSS 图片云端存储
5. 公共字段自动填充（利用自定义注解标识,统一做AOP）
6. 多环境配置解耦（分为:主yml、dev.yml、test.yml、pro.yml）
7. 抽取代码中的字符串、提示词等为常量类（统一存放在 Dining-common 模块当中）
8. 基于JUnit5 + Mockito框架，对7个核心ServiceImpl编写76个单元测试用例，覆盖正常流程、各类边界与异常场景
9. 基于 Knife4j + Swagger2 自动生成接口文档（Controller 层: @Api + @ApiOperation 、 DTO类/VO类的EmployeeLoginDTO/EmployeeLoginVO: @ApiModel + @ApiModelProperty）

#### 注意事项
1. 项目启动前记得检查Linux防火墙是否有放行中间件端口，如RabbitMQ的5672端口、Redis的6379端口等
2. 检查yml配置文件的配置数据是否正确


#### B/C端 功能模块
商家端 B端: （7个接口）

    员工管理        （增改查、分页查询、启用禁用、登录、退出）
    分类管理        （增删改查、分页查询、启用禁用）
    菜品管理        （增删改查、分页查询、启用禁用）    --> 引入Redis （Spring Data Redis 手动式）（当更新菜品数据时,做缓存删除的）
    套餐管理        （增删改查、分页查询、启用禁用）    
    订单管理        （分页查询、各订单数据统计、订单详情、接单、拒单、取消订单、派送订单、完成订单） 
    店铺状态管理     （设置、查询）          --> 缓存至Redis （Spring Data Redis 手动式）
    文件上传        （阿里云OSS）

用户端 C端: （8个接口）

    微信登录
    查询菜品        （根据"分类id"查询）    --> 引入Redis （Spring Data Redis 手动式）
    查询套餐                               --> 引入Redis （Spring Cache 注解式）
    查询分类
    查询店铺状态                           --> 缓存至Redis （Spring Data Redis 手动式）
    管理购物车      （增删查、清空购物车）
    管理订单        （提交订单、订单支付、查询历史订单、再来一单、订单详情、取消订单）   --> 引入RabbitMQ
    管理地址        （增删改查、设置默认）

