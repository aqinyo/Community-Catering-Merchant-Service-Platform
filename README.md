# Dining_service_platform（社区餐饮商户服务平台）

#### 介绍
- 本项目是面向社区餐饮商家与消费者的B/C双端后端服务平台，是基于 Spring Boot 构建增强型模块化的分层单体架构，并引入RabbitMQ、Redis、Sentinel中间件进行轻量级增强，围绕性能和稳定性，设计实现“缓存+异步+限流”的三层性能防护体系，按需引入： Redis 解决高频查询问题、RabbitMQ 实现业务异步解耦、Sentinel 应对接口流量防护。同时基于Vue3结合 AI 辅助开发前端基础管理页面，并独立完成前后端的容器化部署和故障排除。
- 开发时间:2026.04 - 2026.08
- 项目性质:独立后端实战项目
- 项目架构:模块化分层单体架构（controller - service - mapper 三层架构）
- 项目层级:
  - dining-common: 公共通用模块（工具类、常量、异常 等通用代码）
  - dining-model: 数据模型模块（DTO/VO/实体类）
  - dining-server: 核心业务模块（启动类 + Controller + Service + Mapper 等业务逻辑代码） 

#### 项目架构流程
前端 → Nginx → SpringBoot → Redis / RabbitMQ / Sentinel → MySQL

#### 技术栈
- 核心框架: Spring Boot 2.7.3 + Spring MVC
- 数据库框架: MyBatis + Druid连接池 + PageHelper分页插件
- 缓存: Redis
- 消息队列: RabbitMQ
- 安全认证: JWT
- 云存储: 阿里云 OSS
- 版本控制: Git
- 容器: Docker
- 测试与规范: Junit5 + Mockito、Apifox
- 接口文档: SpringDoc OpenAPI 1.7.0  +  Knife4j 3.0.3
- 定时任务: Spring Task
- 监控运维: Spring Boot Actuator 2.7.3
- 限流/降级: Sentinel

### 本地开发-运行环境
- 开发工具: IntelliJ IDEA（Amazon Corretto 1.8.0_452）
- 数据库: MySQL 8.0 （本地）
- 中间件运行操作系统: Linux（CentOS 7）
- 缓存: Redis 6.2.6         
- 消息队列: RabbitMQ 3.9+     
- 容器环境: Docker 26.1.4

### 项目部署-运行环境（模拟生产环境）
- 部署系统: Linux CentOS 7（虚拟机）
- 使用工具: VMware Workstation
- 数据库: MySQL 8.0        （Linux本地部署、普通用户权限）
- 缓存: Redis 6.2.6        （compose编排、Docker容器运行）
- 消息队列: RabbitMQ 3.9+   （compose编排、Docker容器运行）
- 反向代理: Nginx 1.27      （compose编排、Docker容器运行）
- 容器环境: Docker 26.1.4
- 部署编排: Docker Compose
- SSH工具: FinalShell      （普通用户权限）

#### 亮点与细节
1. 规范开发：JWT鉴权、DTO/VO/实体数据隔离、mapper自动代理、基于 RESTful 规范设计接口、 统一响应格式:Result封装
2. 引入 Redis 优化热点数据缓存，并基于Spring Task 实现缓存预热的定时任务（手动式缓存：菜品、店铺状态 / 注解式缓存：套餐）
3. RabbitMQ 异步解耦与延迟消息，保证消息可靠性，并完成全局异常消息的兜底机制
4. 集成阿里云 OSS 图片云端存储
5. 抽取代码中的字符串、提示词等为常量类，统一管理，避免硬编码（统一存放在 Dining-common 模块当中）
6. 基于JUnit5 + Mockito框架，对7个核心ServiceImpl编写76个单元测试用例，覆盖正常流程、各类边界与异常场景
7. 基于 SpringDoc + Knife4j 自动生成接口文档
8. 基于 Actuator 监控项目运行状态（健康检查、环境变量、指标、线程转储等），负责暴露监控端点（/actuator/），能为compose的healthcheck提供监控数据，同时为后续可视化监控提供数据接口（云监控/P+G）
9. 引入 Sentinel 对核心接口进行限流/降级保护，保护系统在高并发场景下的稳定运行，避免服务雪崩

#### 注意事项
1. 项目启动前记得检查Linux防火墙是否有放行中间件端口，如RabbitMQ的5672端口、Redis的6379端口等（docker重启后很容易规则冲突不放行的）
2. 检查yml配置文件的配置数据是否正确
3. 若同一个队列添加新属性，记得去RabbitMQ管理界面删除旧队列，否则会导致消息队列属性不一致，影响新的队列声明
4. 部署时，MySQL独立部署，不能容器化；其余中间件通过docker compose编排，统一容器化部署

#### B/C端 功能模块
商家端 B端: （7个接口）

    员工管理        （增改查、分页查询、启用禁用、登录、退出）
    分类管理        （增删改查、分页查询、启用禁用）
    菜品管理        （增删改查、分页查询、启用禁用）    --> 引入Redis （手动式缓存）（当更新菜品数据时,做缓存删除的,采用最终一致性方案）
    套餐管理        （增删改查、分页查询、启用禁用）    
    订单管理        （分页查询、各订单数据统计、订单详情、接单、拒单、取消订单、派送订单、完成订单） 
    店铺状态管理     （设置、查询）                    --> 引入Redis （手动式缓存）
    文件上传        （阿里云OSS）

用户端 C端: （8个接口）

    微信登录
    查询菜品        （根据"分类id"查询）    --> 引入Redis （手动式缓存）【做了缓存预热】     --> 引入Sentinel（加 流控(QPS) 规则）
    查询套餐                               --> 引入Redis （注解式缓存）【做了缓存预热】     --> 引入Sentinel（加 流控(QPS) 规则）
    查询分类                               --> 引入Redis （注解式缓存）【做了缓存预热】     --> 引入Sentinel（加 流控(QPS) 规则）
    查询店铺状态                           --> 缓存至Redis （手动式缓存）                  --> 引入Sentinel（加 流控(QPS) 规则）
    管理购物车      （增删查、清空购物车）
    管理订单        （提交订单、订单支付、查询历史订单、再来一单、订单详情、取消订单）         --> 引入RabbitMQ、引入Sentinel（对"提交订单、订单支付"接口 加 "流控(线程数隔离) + 降级(异常比例)" 规则）
    管理地址        （增删改查、设置默认）

#### 项目现存瓶颈 & 后续迭代方向
1. 未来业务量上来后，可以把dining-server核心业务子模块拆分成多个子模块，按业务拆分：如订单子模块、用户子模块、菜品子模块等，形成模块化架构而不去强上微服务架构
2. 监控体系目前仅暴露 Actuator 指标，后续可接入“云监控 / Prometheus + Grafana” 实现可视化监控告警
3. 当前缓存规则采用代码硬编码配置，后续可接入 Nacos 配置中心实现限流、缓存配置热更新，无需重启服务（yml配置后续也可以考虑使用Nacos配置中心）
4. 当前的 Sentinel 配置采用代码硬编码进行规则持久化（pull模式的极简固化变种），后续可使用Push模式，接入 Nacos 配置中心实现限流、降级配置热更新，无需重启服务
5. 定时任务当前使用 Spring Task 单机执行，后续流量上涨以及定时任务变多后，可考虑升级为 XXL‑JOB 分布式定时任务，避免单点任务重复执行问题
6. 当前订单模块数据库事务仅保证本地一致性，使用的是最终一致性方案，后续高并发场景下可考虑引入Seata分布式事务组件

