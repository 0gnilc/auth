# Spring Boot 3 测试执行规范

> 文件用途：规范项目测试编写、执行、维护和代码评审中的测试要求。  
> 适用技术栈：Spring Boot 3、Java 17+、MySQL 8、Redis 8、MyBatis-Plus（`com.baomidou`）。  
> 当前文件：`docs/test/testing-guide.md`。

---

## 0. 项目技术栈固定假设

本项目基础技术栈固定为：

```text
Spring Boot 3
Java 17+
MySQL 8
Redis 8
MyBatis-Plus，com.baomidou
JUnit 5
Mockito
AssertJ
MockMvc
Testcontainers
RestAssured
```

编写测试时必须围绕该技术栈设计，不要擅自替换为其他测试栈。

---

## 1. 总体目标

编写测试时，不追求“理论完整”，而是编写能在当前项目中落地执行、能在 CI 中稳定运行、能定位真实问题的测试。

本项目测试分为六类：

```text
1. 纯单元测试：*Test
2. Controller 切片测试：*ControllerTest
3. MyBatis-Plus Mapper / SQL 测试：*MapperIT
4. Redis 行为测试：*CacheIT
5. Spring Boot 集成测试：*IT
6. RANDOM_PORT API 接口测试：*ApiIT
```

Node 构建脚本测试属于工程工具链测试例外，不计入上述 Java 业务测试六分类。仅当被测生产代码本身是 Node 脚本时允许使用 Node 内置 `node:test`，并遵循：

```text
测试放在 owning module 的 scripts/test 下，与被测工程脚本放在同一工具链目录中。
文件命名使用 *.test.mjs。
只验证命令映射、平台选择、退出码传播等工程编排行为。
通过 owning package 的 test:scripts 执行，并纳入 CI。
不得用 Node 测试替代任何应由 JUnit 5、Spring Test 或 Testcontainers 覆盖的 Java 行为。
```

推荐比例：

```text
纯单元测试：30%
Controller 切片测试：10%
Mapper / SQL 测试：20%
Redis 行为测试：10%
Spring Boot 集成测试：20%
RANDOM_PORT API 接口测试：10%
```

所有测试及其专用支持代码都放在所属 Maven 模块的 `src/test` 下，不使用 `src/intg-test` source set。

测试包应镜像目标生产代码包：单类行为测试放在目标类的同名包下，并以目标类命名；只有验证真实协作关系的集成场景才允许覆盖同一边界内的多个类。禁止把不同 `context`、`provider`、`filter`、Controller 或工具类行为集中到模块根包的总括测试类中。

本仓库的集成测试只允许使用 Testcontainers 提供的 MySQL 8 和 Redis 8，不连接本机、开发或共享服务。下文的 `NAMESPACE_RESET` 是未来启用并行隔离时的设计约束，不代表当前允许在共享环境执行测试。

编写测试时必须优先保证：

```text
测试可重复运行
测试不依赖本地开发库
测试不依赖共享测试库
测试不依赖执行顺序
测试失败能定位到明确原因
测试覆盖 MySQL 8 和 Redis 8 的容器行为
```

---

## 2. 最高优先级规则

### 2.1 禁止使用 H2 替代 MySQL 8

本项目生产数据库是 MySQL 8。  
编写 Mapper、SQL、集成测试、接口测试时，必须使用 Testcontainers MySQL 8。

禁止：

```text
H2
内存数据库
本机 MySQL
开发库
共享测试库
```

原因：

```text
MyBatis-Plus 的分页、Wrapper、逻辑删除、乐观锁、自动填充、自定义 XML SQL、唯一索引、datetime/timestamp 行为都可能依赖 MySQL 8。
```

---

### 2.2 Redis 行为测试必须使用 Redis 8

涉及以下逻辑时，必须写 Redis 集成测试：

```text
缓存写入
缓存读取
缓存驱逐
TTL
登录 token
验证码
限流
分布式锁
幂等 key
用户会话
```

禁止：

```text
依赖本机 Redis
依赖共享 Redis
只 mock Redis 就认为覆盖了缓存行为
```

允许：

```text
Testcontainers GenericContainer redis:8-alpine
```

---

### 2.3 不要所有测试都用 `@SpringBootTest`

必须按测试目标选择最轻量的测试方式。

```text
只测 Service 业务分支 -> JUnit 5 + Mockito，不启动 Spring
只测 Controller 参数校验 / JSON / 状态码 -> @WebMvcTest
只测 Mapper / SQL -> @MybatisPlusTest + MySQL 8 Testcontainers
测 Controller + Service + Mapper + MySQL/Redis -> @SpringBootTest + MockMvc + Testcontainers
测 RANDOM_PORT API 合同 -> @SpringBootTest(RANDOM_PORT) + RestAssured + Testcontainers
```

---

### 2.4 RANDOM_PORT API 接口测试必须显式清理数据

`@SpringBootTest(webEnvironment = RANDOM_PORT)` 会启动嵌入式 Web Server。  
测试客户端和服务端不在同一个线程、同一个事务里，因此服务端写入的数据不会随着测试方法上的 `@Transactional` 自动回滚。

因此，编写 `*ApiIT` 时必须显式处理：

```text
MySQL 数据
Redis 数据
必要的消息队列数据
RestAssured 全局状态
测试基础数据 seed
```

默认策略：

```text
*ApiIT 默认使用 BASELINE_RESET。
BASELINE_RESET 不是重启容器，也不是重建 schema。
它表示恢复到“测试初始化状态”：
  1. 表结构和 migration 元数据保留。
  2. 清空业务可变表。
  3. 清空 Redis 当前 DB。
  4. 重新插入必要基础数据。
  5. 自增 ID 尽量恢复稳定。
```

落地规则：

```text
1. *ApiIT 必须继承 ApiTestSupport，或使用 @ApiTest 等价组合注解。
2. 每个测试方法执行前必须恢复到测试初始化状态。
3. 测试方法执行后必须兜底清理，避免失败测试污染后续用例。
4. 不允许依赖 @Transactional 回滚RANDOM_PORT API 请求产生的数据。
5. 不允许连接开发库、共享测试库、共享 Redis。
6. Testcontainers 独占环境下允许 TRUNCATE 业务表和 Redis FLUSHDB。
7. 本仓库不在共享环境运行测试；未来若启用并行隔离，必须先实现 NAMESPACE_RESET 并同步更新仓库规范。
```

---

## 3. 测试类型决策树

### 3.1 新增或修改 Service

如果变更涉及：

```text
业务规则
状态流转
权限判断
异常分支
默认值填充
参数组合校验
调用 Mapper 前后的逻辑
Redis key 生成或缓存分支
```

必须新增或更新：

```text
*ServiceTest
```

测试方式：

```text
JUnit 5 + Mockito + AssertJ
mock MyBatis-Plus Mapper
mock StringRedisTemplate / RedisTemplate
mock 外部服务
不启动 Spring
```

---

### 3.2 新增或修改 Controller

如果变更涉及：

```text
路由
请求参数
请求体 DTO
Bean Validation
响应 JSON
统一异常
状态码
```

必须新增或更新：

```text
*ControllerTest
```

优先使用：

```java
@WebMvcTest(XxxController.class)
```

如果 Controller 强依赖 JWT Filter、安全上下文、全局异常配置，且 `@WebMvcTest` 配置复杂，可以使用：

```java
@SpringBootTest
@AutoConfigureMockMvc
```

但 Service 必须 mock，避免变成完整集成测试。

---

### 3.3 新增或修改 Mapper / XML SQL

如果变更涉及：

```text
BaseMapper 使用
LambdaQueryWrapper
QueryWrapper
UpdateWrapper
自定义 XML SQL
分页插件
逻辑删除
乐观锁
自动填充
TypeHandler
枚举映射
唯一索引
多租户插件
数据权限插件
```

必须新增或更新：

```text
*MapperIT
```

测试方式：

```text
@MybatisPlusTest
@AutoConfigureTestDatabase(replace = NONE)
Testcontainers MySQL 8
```

如果 `@MybatisPlusTest` 与项目配置冲突，可以退而使用：

```text
@SpringBootTest + Testcontainers MySQL 8
```

稳定优先，切片优化第二。

---

### 3.4 新增或修改缓存逻辑

如果变更涉及：

```text
@Cacheable
@CacheEvict
@CachePut
StringRedisTemplate
RedisTemplate
CacheManager
验证码
token
限流
分布式锁
```

必须新增或更新：

```text
*CacheIT
```

测试方式：

```text
@SpringBootTest
Testcontainers Redis 8
Redis 8 容器读写断言
```

必须断言：

```text
key 是否存在
value 是否正确
TTL 是否符合预期
更新/删除后 key 是否被驱逐
异常时锁是否释放
```

---

### 3.5 新增或修改核心业务接口

如果变更属于核心链路，例如：

```text
登录
创建核心业务对象
支付 / 下单 / 审批 / 状态流转
权限访问
分页查询
统一异常返回格式
```

必须新增或更新：

```text
*ApiIT
```

测试方式：

```text
@SpringBootTest(webEnvironment = RANDOM_PORT)
RestAssured
Testcontainers MySQL 8
Testcontainers Redis 8
BASELINE_RESET 或 NAMESPACE_RESET
显式清理数据
```

数量要求：

```text
每个服务保留 5 到 10 条高价值 ApiIT，不要把所有 CRUD 都写成 ApiIT。
```

---

## 4. 测试支持代码的分层与存放位置

本节是本规范的核心之一。  
数据清理代码不能全部塞进公共模块，也不能全部贴在测试类里。必须按职责分层。

### 4.1 分层原则

一句话原则：

```text
公共测试基础设施负责“怎么清理”。
业务模块测试支持负责“清理什么业务数据、如何构造业务数据”。
```

公共测试基础设施应该是无业务语义的。  
它可以提供清理流程、清理模式、安全检查、通用 MySQL / Redis 清理能力和扩展接口。  
它不应该知道 `sys_user`、`biz_order`、`order_item`、`role_id`、`WAIT_PAY` 这些业务含义。

---

### 4.2 公共测试基础设施放哪里

单模块项目：

```text
src/test/java/com/example/project/testsupport
```

多模块 Maven 项目：

```text
project-test-support
└── src/main/java/com/example/testsupport
```

多模块 Gradle 项目：

```text
优先使用 java-test-fixtures
```

公共测试基础设施包括：

```text
CleanupMode
CleanTestData
TestDataResetListener
TestDataResetManager
DatabaseCleaner
RedisCleaner
TestEnvironmentGuard
TestNamespaceHolder
ContainersTestSupport
IntegrationTest
ApiTest
ApiTestSupport
BaselineDataSeeder 接口
NamespaceDataCleaner 接口
```

公共测试基础设施允许做：

```text
提供清理模式
调度清理流程
校验当前环境是否允许清理
清空业务可变表
清空 Redis 当前 DB
按 Redis key prefix 删除
调用所有 BaselineDataSeeder
调用所有 NamespaceDataCleaner
```

公共测试基础设施不允许做：

```text
引用业务 Mapper
引用业务 Entity
引用业务枚举
写死业务表名
写死业务字段名
写死某个用户、订单、角色、状态
实现某个模块的父子表删除顺序
```

---

### 4.3 应用级基础数据 seed 放哪里

应用级基础数据是有业务语义的，但通常整个应用都需要。

例如：

```text
admin 用户
默认租户
默认角色
默认权限
默认菜单
默认字典
默认测试配置
测试 OAuth client
```

单应用模块项目：

```text
app
└── src/test/java/com/example/app/support/seed
    └── AppBaselineDataSeeder.java
```

多模块项目，业务 API 测试及其 seed 默认放在拥有该 API 的功能模块：

```text
user-module
└── src/test/java/com/example/user
    ├── api/UserApiIT.java
    └── support/seed/UserApiBaselineDataSeeder.java
```

只有无法由单个功能模块启动、必须验证最终应用跨模块组合时，才把该场景的 seed 放在启动模块。多个模块确实需要同一套业务 seed 时，可以建立独立的应用测试支持模块：

```text
app-test-support
└── src/main/java/com/example/app/testsupport
    └── AppBaselineDataSeeder.java
```

注意：

```text
project-test-support：纯测试基础设施，无业务依赖。
app-test-support：应用级测试支持，有业务依赖，只能 test scope 使用。
```

---

### 4.4 模块级测试支持放哪里

单独某个模块、某个表、某个功能的数据构造和清理，放在当前模块测试代码附近。

示例：

```text
user-module
└── src/test/java/com/example/user/support
    ├── UserTestDataFactory.java
    ├── UserNamespaceDataCleaner.java
    ├── UserAssertions.java
    └── UserTestSupportConfig.java

order-module
└── src/test/java/com/example/order/support
    ├── OrderTestDataFactory.java
    ├── OrderNamespaceDataCleaner.java
    ├── OrderAssertions.java
    └── OrderTestSupportConfig.java
```

这些代码有明确业务含义：

```text
用户测试数据怎么构造
订单和订单明细怎么构造
订单清理时先删 order_item 再删 order
用户清理时先删 user_role 再删 user
某个业务状态要怎么准备
某个功能需要哪些前置数据
```

它们不应该放进 `project-test-support`，否则公共模块会被业务污染。

---

### 4.5 单个测试类私有清理逻辑放哪里

如果清理逻辑只服务于一个测试类，直接放在测试类里。

适合放测试类内部：

```text
只被一个测试类使用
逻辑很短
和测试场景强绑定
抽出去反而降低阅读性
```

应该抽到模块 `support` 的情况：

```text
被同模块 2 个以上测试类复用
清理顺序复杂
测试数据构造复杂
多个测试都需要同样的前置数据
```

---

### 4.6 最佳存放决策表

| 类型 | 是否有业务依赖 | 使用范围 | 推荐位置 |
|---|---:|---|---|
| `CleanupMode` / `CleanTestData` | 否 | 全项目 | `project-test-support` |
| `DatabaseCleaner` / `RedisCleaner` | 否 | 全项目 | `project-test-support` |
| `TestDataResetManager` | 否 | 全项目 | `project-test-support` |
| `BaselineDataSeeder` 接口 | 否 | 全项目 | `project-test-support` |
| `NamespaceDataCleaner` 接口 | 否 | 全项目 | `project-test-support` |
| 默认 admin / 角色 / 权限 seed | 是 | 功能模块 API | 所属功能模块 `src/test/.../support` |
| `UserTestDataFactory` | 是 | user 模块 | `user-module/src/test/.../support` |
| `OrderNamespaceDataCleaner` | 是 | order 模块 | `order-module/src/test/.../support` |
| 只服务单个测试类的 helper | 是 | 单个测试类 | 测试类内部 |

---

## 5. 推荐目录结构

### 5.1 单模块项目

```text
src/test/java
└── com/example/project
    ├── testsupport
    │   ├── container
    │   │   └── ContainersTestSupport.java
    │   ├── annotation
    │   │   ├── IntegrationTest.java
    │   │   └── ApiTest.java
    │   ├── cleanup
    │   │   ├── CleanupMode.java
    │   │   ├── CleanTestData.java
    │   │   ├── TestDataResetListener.java
    │   │   ├── TestDataResetManager.java
    │   │   ├── DatabaseCleaner.java
    │   │   ├── RedisCleaner.java
    │   │   ├── TestDataSeeder.java
    │   │   ├── TestEnvironmentGuard.java
    │   │   └── TestNamespaceHolder.java
    │   ├── seed
    │   │   └── AppBaselineDataSeeder.java
    │   ├── ApiTestSupport.java
    │   └── TestDataFactory.java
    ├── user
    │   ├── UserServiceTest.java
    │   ├── UserControllerTest.java
    │   ├── UserMapperIT.java
    │   ├── UserResourceIT.java
    │   ├── UserCacheIT.java
    │   └── UserApiIT.java
    └── auth
        ├── AuthServiceTest.java
        ├── AuthControllerTest.java
        └── AuthApiIT.java

src/test/resources
├── application-test.yml
├── sql
│   ├── clean.sql
│   └── seed.sql
└── postman
    ├── project.postman_collection.json
    └── local.postman_environment.json
```

---

### 5.2 多模块 Maven 项目

```text
root
├── pom.xml
├── project-test-support
│   ├── pom.xml
│   └── src/main/java/com/example/testsupport
│       ├── cleanup
│       │   ├── CleanupMode.java
│       │   ├── CleanTestData.java
│       │   ├── TestDataResetManager.java
│       │   ├── TestDataResetListener.java
│       │   ├── DatabaseCleaner.java
│       │   ├── RedisCleaner.java
│       │   ├── TestEnvironmentGuard.java
│       │   ├── TestNamespaceHolder.java
│       │   ├── BaselineDataSeeder.java
│       │   └── NamespaceDataCleaner.java
│       ├── container
│       │   └── ContainersTestSupport.java
│       └── annotation
│           ├── IntegrationTest.java
│           └── ApiTest.java
│
├── app-bootstrap
│   └── src/test/java/com/example/app
│       └── ApplicationContextIT.java
│
├── user-module
│   └── src/test/java/com/example/user
│       ├── api
│       │   └── UserApiIT.java
│       └── support
│           ├── UserApiBaselineDataSeeder.java
│           ├── UserTestDataFactory.java
│           ├── UserNamespaceDataCleaner.java
│           └── UserTestSupportConfig.java
│
└── order-module
    └── src/test/java/com/example/order
        ├── api
        │   └── OrderApiIT.java
        └── support
            ├── OrderApiBaselineDataSeeder.java
            ├── OrderTestDataFactory.java
            ├── OrderNamespaceDataCleaner.java
            └── OrderTestSupportConfig.java
```

依赖方向：

```text
user-module test scope -> project-test-support
order-module test scope -> project-test-support
app-bootstrap test scope -> project-test-support
```

禁止：

```text
project-test-support -> user-module
project-test-support -> order-module
project-test-support -> app-bootstrap
```

Maven 依赖示例：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>project-test-support</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

不推荐把通用测试基础设施放在根模块 `src/test/java`：

```text
根模块通常只是 aggregator/parent，不是业务模块。
子模块测试编译时不会自动拿到根模块 test-classes。
IDE 和 CI 的行为容易不一致。
根模块 test 代码无法表达清晰的 test scope 依赖关系。
```

---

### 5.3 Gradle 多模块项目

Gradle 项目优先使用 `java-test-fixtures`。

```groovy
plugins {
    id 'java-library'
    id 'java-test-fixtures'
}

dependencies {
    testImplementation(testFixtures(project(":common")))
}
```

如果测试基础设施完全独立，也可以采用：

```text
project-test-support 模块
```

---

## 6. 测试依赖规则

### 6.1 Maven 依赖

```xml
<dependencies>
    <!-- JUnit 5、Mockito、AssertJ、Spring Test、MockMvc -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- MyBatis-Plus 官方测试 starter，提供 @MybatisPlusTest -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot 3.1+ Testcontainers 集成 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Redis 使用 GenericContainer，因此保留 testcontainers 核心依赖 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- RANDOM_PORT API 接口测试 -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 如果项目使用 Spring Security，建议加 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 7. 统一测试基础设施

### 7.1 ContainersTestSupport

用于统一启动 MySQL 8 和 Redis 8。

```java
@Testcontainers(disabledWithoutDocker = true)
public abstract class ContainersTestSupport {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("app_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }
}
```

要求：

```text
MySQL 镜像使用 mysql:8.x。
Redis 镜像使用 redis:8-alpine 或项目认可的 Redis 8 镜像。
如果 CI Docker 不可用，不要切换到 H2；应报告集成测试无法运行。
```

---

### 7.2 IntegrationTest 组合注解

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public @interface IntegrationTest {
}
```

使用：

```java
@IntegrationTest
class UserResourceIT extends ContainersTestSupport {
}
```

---

### 7.3 CleanupMode

```java
public enum CleanupMode {

    /**
     * 不清理。
     * 用于单元测试、Controller 切片测试。
     */
    NONE,

    /**
     * 依赖 Spring 测试事务自动回滚。
     * 用于 Mapper 测试、MockMvc 集成测试。
     * 使用该模式时，测试类仍然需要自己声明 @Transactional。
     */
    TRANSACTION_ROLLBACK,

    /**
     * 只清 Redis。
     * 用于 MockMvc 集成测试里数据库走事务回滚，但 Redis 需要显式清理的场景。
     */
    REDIS_CLEAN,

    /**
     * 恢复到测试初始化状态。
     * 清空业务表、清空 Redis、重新 seed 基础数据。
     * 用于 Testcontainers 独占环境下的RANDOM_PORT API 测试。
     */
    BASELINE_RESET,

    /**
     * 命名空间清理。
     * 只删除 testRunId / tenantId / keyPrefix 相关数据。
     * 用于共享测试环境或并行测试。
     */
    NAMESPACE_RESET,

    /**
     * 执行指定 SQL 脚本。
     * 少量特殊场景可使用 @Sql，不作为默认清理方式。
     */
    SQL_SCRIPT
}
```

---

### 7.4 CleanTestData 注解

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CleanTestData {

    CleanupMode mode() default CleanupMode.BASELINE_RESET;

    boolean beforeEach() default true;

    boolean afterEach() default true;

    /**
     * BASELINE_RESET 后是否重新插入基础数据。
     */
    boolean seed() default true;

    /**
     * NAMESPACE_RESET 使用。
     */
    String namespace() default "";
}
```

---

### 7.5 扩展接口：BaselineDataSeeder

公共模块只定义接口，不写业务实现。

```java
public interface BaselineDataSeeder {

    /**
     * 恢复测试初始化状态需要的基础数据。
     * 例如角色、权限、测试用户、租户、字典。
     */
    void seedBaseline();
}
```

业务基线实现放在拥有对应 API 的功能模块 `src/test/java/.../support/seed`。只有最终应用组合场景无法由功能模块承载时，才放在 `app-bootstrap/src/test`：

```java
@TestConfiguration
public class AppTestSeedConfig {

    @Bean
    BaselineDataSeeder appBaselineDataSeeder(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        return () -> {
            RoleDO adminRole = new RoleDO();
            adminRole.setId(1L);
            adminRole.setCode("ADMIN");
            adminRole.setName("管理员");
            roleMapper.insert(adminRole);

            UserDO admin = new UserDO();
            admin.setId(1L);
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            userMapper.insert(admin);

            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(1L);
            userRole.setRoleId(1L);
            userRoleMapper.insert(userRole);
        };
    }
}
```

---

### 7.6 扩展接口：NamespaceDataCleaner

公共模块只定义接口，不写业务实现。

```java
public interface NamespaceDataCleaner {

    /**
     * 数字越小越先执行。
     * 如果涉及父子表，子表 cleaner 应该排在父表之前。
     */
    int order();

    /**
     * 清理当前 namespace 下的数据。
     */
    void clean(String namespace);
}
```

模块级实现示例，放在当前模块 `src/test/java/.../support`：

```java
public class OrderNamespaceDataCleaner implements NamespaceDataCleaner {

    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    public OrderNamespaceDataCleaner(
            OrderItemMapper orderItemMapper,
            OrderMapper orderMapper
    ) {
        this.orderItemMapper = orderItemMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void clean(String namespace) {
        orderItemMapper.delete(
            Wrappers.<OrderItemDO>lambdaQuery()
                .eq(OrderItemDO::getTestRunId, namespace)
        );

        orderMapper.delete(
            Wrappers.<OrderDO>lambdaQuery()
                .eq(OrderDO::getTestRunId, namespace)
        );
    }
}
```

模块测试配置：

```java
@TestConfiguration
public class OrderTestSupportConfig {

    @Bean
    NamespaceDataCleaner orderNamespaceDataCleaner(
            OrderItemMapper orderItemMapper,
            OrderMapper orderMapper
    ) {
        return new OrderNamespaceDataCleaner(orderItemMapper, orderMapper);
    }

    @Bean
    OrderTestDataFactory orderTestDataFactory(OrderMapper orderMapper) {
        return new OrderTestDataFactory(orderMapper);
    }
}
```

---

### 7.7 ApiTest 组合注解

RANDOM_PORT API 测试建议使用组合注解，而不是每个测试类重复贴配置。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestExecutionListeners(
    listeners = TestDataResetListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@CleanTestData(
    mode = CleanupMode.BASELINE_RESET,
    beforeEach = true,
    afterEach = true,
    seed = true
)
public @interface ApiTest {
}
```

使用：

```java
@ApiTest
@Import(AppTestSeedConfig.class)
class UserApiIT extends ApiTestSupport {
}
```

---

### 7.8 TestDataResetListener

Spring TestContext 的 `TestExecutionListener` 是统一清理入口。  
所有清理逻辑都通过监听器进入 `TestDataResetManager`，不要在每个测试类复制清理代码。

```java
public class TestDataResetListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        CleanTestData cleanTestData = findAnnotation(testContext);

        if (cleanTestData == null || !cleanTestData.beforeEach()) {
            return;
        }

        TestDataResetManager resetManager =
            testContext.getApplicationContext().getBean(TestDataResetManager.class);

        resetManager.resetBefore(cleanTestData);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        CleanTestData cleanTestData = findAnnotation(testContext);

        if (cleanTestData == null || !cleanTestData.afterEach()) {
            return;
        }

        TestDataResetManager resetManager =
            testContext.getApplicationContext().getBean(TestDataResetManager.class);

        resetManager.resetAfter(cleanTestData);
    }

    private CleanTestData findAnnotation(TestContext testContext) {
        return AnnotatedElementUtils.findMergedAnnotation(
            testContext.getTestClass(),
            CleanTestData.class
        );
    }
}
```

---

### 7.9 TestDataResetManager

```java
@Component
public class TestDataResetManager {

    private final TestEnvironmentGuard environmentGuard;
    private final DatabaseCleaner databaseCleaner;
    private final RedisCleaner redisCleaner;
    private final List<BaselineDataSeeder> seeders;
    private final List<NamespaceDataCleaner> namespaceCleaners;

    public TestDataResetManager(
            TestEnvironmentGuard environmentGuard,
            DatabaseCleaner databaseCleaner,
            RedisCleaner redisCleaner,
            List<BaselineDataSeeder> seeders,
            List<NamespaceDataCleaner> namespaceCleaners
    ) {
        this.environmentGuard = environmentGuard;
        this.databaseCleaner = databaseCleaner;
        this.redisCleaner = redisCleaner;
        this.seeders = seeders;
        this.namespaceCleaners = namespaceCleaners;
    }

    public void resetBefore(CleanTestData config) {
        environmentGuard.assertSafeToClean();

        switch (config.mode()) {
            case NONE, TRANSACTION_ROLLBACK -> {
                // 什么都不做。
                // TRANSACTION_ROLLBACK 由 Spring 测试事务处理。
            }
            case REDIS_CLEAN -> redisCleaner.cleanAll();
            case BASELINE_RESET -> resetToBaseline(config.seed());
            case NAMESPACE_RESET -> resetNamespace(resolveNamespace(config));
            case SQL_SCRIPT -> {
                throw new UnsupportedOperationException("SQL_SCRIPT 建议使用 @Sql 管理");
            }
        }
    }

    public void resetAfter(CleanTestData config) {
        environmentGuard.assertSafeToClean();

        switch (config.mode()) {
            case BASELINE_RESET -> {
                redisCleaner.cleanAll();
                databaseCleaner.cleanBusinessTables();
            }
            case NAMESPACE_RESET -> resetNamespace(resolveNamespace(config));
            case REDIS_CLEAN -> redisCleaner.cleanAll();
            default -> {
                // NONE / TRANSACTION_ROLLBACK / SQL_SCRIPT 不处理
            }
        }
    }

    private void resetToBaseline(boolean seed) {
        // 推荐顺序：
        // 1. 先清 Redis，避免旧缓存影响 seed 或测试。
        // 2. 清 MySQL 业务表。
        // 3. seed 基础数据。
        // 4. 再清一次 Redis，避免 seed 过程触发缓存写入。
        redisCleaner.cleanAll();
        databaseCleaner.cleanBusinessTables();

        if (seed) {
            seeders.forEach(BaselineDataSeeder::seedBaseline);
        }

        redisCleaner.cleanAll();
    }

    private void resetNamespace(String namespace) {
        redisCleaner.cleanByPrefix("test:" + namespace + ":");

        namespaceCleaners.stream()
            .sorted(Comparator.comparingInt(NamespaceDataCleaner::order))
            .forEach(cleaner -> cleaner.clean(namespace));
    }

    private String resolveNamespace(CleanTestData config) {
        if (!config.namespace().isBlank()) {
            return config.namespace();
        }

        return TestNamespaceHolder.currentNamespace();
    }
}
```

关键边界：

```text
TestDataResetManager 不知道 User、Order、Role、Menu。
它只知道有一批 BaselineDataSeeder。
它只知道有一批 NamespaceDataCleaner。
具体业务数据怎么 seed、怎么按 namespace 删除，由业务模块提供实现。
```

---

### 7.10 ApiTestSupport

```java
public abstract class ApiTestSupport extends FullStackContainerSupport {

    @LocalServerPort
    protected int port;

    @BeforeEach
    final void resetRestAssuredBeforeEachTest() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    final void resetRestAssuredAfterEachTest() {
        RestAssured.reset();
    }
}
```

说明：

```text
数据清理由 TestDataResetListener 完成。
ApiTestSupport 只负责RANDOM_PORT API 测试的通用客户端状态。
不要在 ApiTestSupport 里写业务 seed。
```

---

### 7.11 DatabaseCleaner

RANDOM_PORT API 接口测试、异步测试、复杂集成测试需要显式清理数据库。  
MySQL 的 `FOREIGN_KEY_CHECKS` 是会话级变量，所以禁用外键、truncate、恢复外键必须尽量在同一个 JDBC Connection 中完成。

```java
@Component
public class DatabaseCleaner {

    private static final Set<String> EXCLUDED_TABLES = Set.of(
        "flyway_schema_history",
        "databasechangelog",
        "databasechangeloglock"
    );

    private static final List<String> EXCLUDED_PREFIXES = List.of(
        "qrtz_",
        "undo_log"
    );

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanBusinessTables() {
        List<String> tables = findCleanableTables();

        if (tables.isEmpty()) {
            return;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");

                for (String table : tables) {
                    statement.addBatch("TRUNCATE TABLE " + quoteIdentifier(table));
                }

                statement.executeBatch();
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }

            return null;
        });
    }

    private List<String> findCleanableTables() {
        return jdbcTemplate.queryForList("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
            """, String.class)
            .stream()
            .filter(this::isCleanable)
            .sorted()
            .toList();
    }

    private boolean isCleanable(String table) {
        String lower = table.toLowerCase(Locale.ROOT);

        if (EXCLUDED_TABLES.contains(lower)) {
            return false;
        }

        for (String prefix : EXCLUDED_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
```

要求：

```text
优先动态发现表，避免新增表后忘记维护清理列表。
必须排除 flyway_schema_history / databasechangelog 等迁移元数据表。
如果有必须保留的字典表，应加入排除列表，并在测试前显式准备业务数据。
不要在共享数据库上运行该 cleaner；它只能用于 Testcontainers 或专用测试库。
DatabaseCleaner 不实现按业务 namespace 删除，namespace 删除由业务模块 NamespaceDataCleaner 实现。
```

---

### 7.12 RedisCleaner

```java
@Component
public class RedisCleaner {

    private final StringRedisTemplate redisTemplate;

    public RedisCleaner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cleanAll() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();

        if (factory == null) {
            return;
        }

        try (RedisConnection connection = factory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    public void cleanByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }

        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();

        if (factory == null) {
            return;
        }

        byte[] pattern = redisTemplate.getStringSerializer()
            .serialize(prefix + "*");

        try (RedisConnection connection = factory.getConnection();
             Cursor<byte[]> cursor = connection.scan(
                 ScanOptions.scanOptions()
                     .match(new String(pattern, StandardCharsets.UTF_8))
                     .count(1000)
                     .build()
             )) {

            List<byte[]> batch = new ArrayList<>(500);

            while (cursor.hasNext()) {
                batch.add(cursor.next());

                if (batch.size() >= 500) {
                    deleteBatch(connection, batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                deleteBatch(connection, batch);
            }
        }
    }

    private void deleteBatch(RedisConnection connection, List<byte[]> keys) {
        connection.keyCommands().del(keys.toArray(byte[][]::new));
    }
}
```

要求：

```text
只有在 Redis Testcontainers 或专用测试 Redis 中允许 flushDb。
共享 Redis 必须使用 key prefix 清理。
RANDOM_PORT API 接口测试必须同时清理 MySQL 和 Redis。
```

---

### 7.13 TestEnvironmentGuard

清理器具有破坏性，必须有环境保护。

```java
@Component
public class TestEnvironmentGuard {

    private final Environment environment;
    private final DataSource dataSource;

    public TestEnvironmentGuard(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    public void assertSafeToClean() {
        assertTestProfile();
        assertCleanupExplicitlyEnabled();
        assertJdbcUrlLooksSafe();
    }

    private void assertTestProfile() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());

        if (!profiles.contains("test")) {
            throw new IllegalStateException(
                "Refuse to clean data because active profile does not contain 'test'"
            );
        }
    }

    private void assertCleanupExplicitlyEnabled() {
        boolean enabled = environment.getProperty(
            "app.test.cleanup.enabled",
            Boolean.class,
            false
        );

        if (!enabled) {
            throw new IllegalStateException(
                "Refuse to clean data because app.test.cleanup.enabled is not true"
            );
        }
    }

    private void assertJdbcUrlLooksSafe() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();

            boolean safe =
                url.contains("localhost")
                    || url.contains("127.0.0.1")
                    || url.contains("testcontainers")
                    || url.contains("_test");

            if (!safe) {
                throw new IllegalStateException(
                    "Refuse to clean data because JDBC URL does not look safe: " + url
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to inspect JDBC URL", e);
        }
    }
}
```

`application-test.yml`：

```yaml
app:
  test:
    cleanup:
      enabled: true
```

---

### 7.14 TestNamespaceHolder

```java
public final class TestNamespaceHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TestNamespaceHolder() {
    }

    public static void set(String namespace) {
        CURRENT.set(namespace);
    }

    public static String currentNamespace() {
        String namespace = CURRENT.get();

        if (namespace == null) {
            namespace = "test-" + UUID.randomUUID();
            CURRENT.set(namespace);
        }

        return namespace;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

业务测试数据必须带 namespace：

```java
UserDO user = new UserDO();
user.setUsername("user_" + TestNamespaceHolder.currentNamespace());
user.setTestRunId(TestNamespaceHolder.currentNamespace());
userMapper.insert(user);
```

Redis key 必须带 namespace：

```java
String key = "test:" + TestNamespaceHolder.currentNamespace() + ":login-token:" + userId;
redisTemplate.opsForValue().set(key, token);
```

---

### 7.15 TestDataFactory

测试数据构造必须集中管理，避免每个测试类硬编码大量 JSON 或实体字段。

模块级工厂放当前模块测试代码附近：

```java
public class UserTestDataFactory {

    private final UserMapper userMapper;

    public UserTestDataFactory(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Long createNormalUser(String username) {
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setTestRunId(TestNamespaceHolder.currentNamespace());
        userMapper.insert(user);
        return user.getId();
    }
}
```

要求：

```text
通用工厂基类可放通用测试支持模块。
模块专用工厂放当前模块 src/test/java/.../support。
不要在测试中依赖固定执行顺序。
不要复用其他测试方法创建的数据。
```

---

### 7.16 并行测试兼容规则

`BASELINE_RESET` 和方法级并行天然冲突。

如果两个 `*ApiIT` 同时跑：

```text
A 测试正在创建订单
B 测试执行 TRUNCATE 全库
A 测试随机失败
```

可选方案：

```text
1. 初期最稳：MapperIT / ResourceIT / ApiIT 串行执行。
2. 如果开启并行：全库清理类测试加 @ResourceLock。
3. 成熟阶段：引入 NAMESPACE_RESET。
```

JUnit 资源锁示例：

```java
@ResourceLock(value = "mysql-redis", mode = ResourceAccessMode.READ_WRITE)
@ApiTest
class UserApiIT extends ApiTestSupport {
}
```

---

## 8. 单元测试编写规则

### 8.1 Service 单元测试

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_whenEmailExists_shouldThrowBusinessException() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> userService.createUser(new CreateUserCommand("alice@example.com", "Alice"))
        );

        assertThat(ex.getCode()).isEqualTo("USER_EMAIL_EXISTS");
        verify(userMapper, never()).insert(any());
    }
}
```

必须做到：

```text
不启动 Spring。
只测当前类。
所有外部依赖 mock。
必须断言业务结果。
必须 verify 关键副作用。
```

---

### 8.2 缓存 Key / 工具类单元测试

```java
class UserCacheKeyTest {

    @Test
    void userDetailKey_shouldBeStable() {
        String key = UserCacheKeys.userDetail(100L);

        assertThat(key).isEqualTo("user:detail:100");
    }
}
```

Redis key 规则如果变更，必须有单元测试兜底。

---

## 9. Controller 测试编写规则

### 9.1 简单 Controller 用 `@WebMvcTest`

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void createUser_whenRequestInvalid_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "",
                      "name": ""
                    }
                """))
            .andExpect(status().isBadRequest());
    }
}
```

必须测试：

```text
请求路径
HTTP method
参数校验
JSON 响应
异常返回
```

---

### 9.2 有安全链路时用完整 Spring Context + MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSecurityTest extends ContainersTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUser_whenAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }
}
```

如果使用 Spring Security，推荐使用 `spring-security-test` 提供的测试能力。  
如果使用 Sa-Token、自研 JWT 或网关鉴权，应提供项目自己的 `TestLoginHelper` 或 `TestTokenFactory`。

---

## 10. Mapper / SQL 测试编写规则

```java
@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserMapperIT extends ContainersTestSupport {

    @Autowired
    private UserMapper userMapper;

    @Test
    void selectPage_shouldReturnPagedUsers() {
        UserDO user = new UserDO();
        user.setUsername("alice");
        userMapper.insert(user);

        Page<UserDO> page = userMapper.selectPage(
            Page.of(1, 10),
            Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, "alice")
        );

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getTotal()).isEqualTo(1);
    }
}
```

必须覆盖：

```text
自定义 XML SQL
复杂 Wrapper
分页 total / records
逻辑删除
乐观锁
唯一索引
自动填充
TypeHandler
枚举映射
```

---

## 11. 集成测试编写规则

集成测试主力使用 MockMvc，不启动 RANDOM_PORT。

```java
@IntegrationTest
@Transactional
class UserResourceIT extends ContainersTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void createUser_shouldPersistToMysql() throws Exception {
        CreateUserRequest request = new CreateUserRequest("alice@example.com", "Alice");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("alice@example.com"));

        Long count = userMapper.selectCount(
            Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getEmail, "alice@example.com")
        );

        assertThat(count).isEqualTo(1L);
    }
}
```

推荐：

```text
没有 Redis 副作用 -> @Transactional 回滚。
有 Redis 副作用 -> @Transactional + @CleanTestData(mode = REDIS_CLEAN)。
有异步或真实线程副作用 -> 不依赖事务，改用 BASELINE_RESET 或明确清理。
```

---

## 12. Redis 集成测试编写规则

```java
@IntegrationTest
@CleanTestData(mode = CleanupMode.REDIS_CLEAN, beforeEach = true, afterEach = true)
class UserCacheIT extends ContainersTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void getUser_shouldWriteCache() {
        UserDTO user = userService.getUser(1L);

        assertThat(redisTemplate.hasKey("user:detail:1")).isTrue();
        assertThat(redisTemplate.getExpire("user:detail:1")).isGreaterThan(0);
    }
}
```

必须断言：

```text
key 是否存在
value 是否正确
TTL 是否正确
更新后是否失效
删除后是否失效
异常时锁是否释放
```

---

## 13. RANDOM_PORT API 接口测试编写规则

RANDOM_PORT API 接口测试只覆盖少量核心链路，不用于覆盖所有 CRUD。  
默认使用 `@ApiTest + ApiTestSupport`，清理策略使用 `BASELINE_RESET`。

```java
@ApiTest
@Import({AppTestSeedConfig.class, UserTestSupportConfig.class})
class UserApiIT extends ApiTestSupport {

    @Autowired
    private UserTestDataFactory userTestDataFactory;

    @BeforeEach
    void seedCaseData() {
        // 这里发生在统一 BASELINE_RESET 和基础 seed 之后。
        userTestDataFactory.createNormalUser("case_user_001");
    }

    @Test
    void createAndQueryUser_shouldWorkThroughApi() {
        String token = loginAsAdmin();

        Integer userId =
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "email": "alice@example.com",
                      "name": "Alice"
                    }
                """)
            .when()
                .post("/api/users")
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.email", equalTo("alice@example.com"))
                .extract()
                .path("data.id");

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .when()
            .get("/api/users/{id}", userId)
        .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.email", equalTo("alice@example.com"));
    }

    private String loginAsAdmin() {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "admin",
                  "password": "admin123"
                }
            """)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("data.accessToken");
    }
}
```

必须覆盖少量高价值场景：

```text
应用 RANDOM_PORT 启动
登录成功获取 token
携带 token 访问成功
不携带 token 返回 401
角色不足返回 403
创建核心业务对象
查询核心业务对象
核心状态流转
统一错误格式
Redis 缓存写入或失效的关键链路
```

数据清理要求：

```text
1. 默认使用 BASELINE_RESET。
2. 不允许在 *ApiIT 上依赖 @Transactional 回滚。
3. 测试前恢复到测试初始化状态：
   - 清 Redis
   - 清 MySQL 业务表
   - seed 基础数据
   - 再清 Redis
4. 测试自己的预置数据必须在统一清理和基础 seed 之后创建。
5. 测试后再次清理 Redis 和 MySQL。
6. 共享测试环境或并行测试环境必须切换到 NAMESPACE_RESET。
7. 不要只清理“本次测试新增或变更的数据”，除非该测试明确使用 NAMESPACE_RESET。
```

---

## 14. Postman / Newman 规则

如果项目存在 `docs/postman`，修改接口时必须同步更新 Postman Collection。

推荐结构：

```text
docs/postman
├── project.local.postman_environment.json
└── project.postman_collection.json
```

推荐流程：

```text
Auth Flow
- 登录
- 当前用户信息
- 刷新 token，如果有

Admin Flow
- 创建用户
- 查询用户
- 禁用用户
- 恢复用户

Business Flow
- 创建核心业务对象
- 查询列表
- 查询详情
- 更新状态
- 删除或关闭

Error Flow
- 未登录 401
- 无权限 403
- 参数错误 400
- 资源不存在 404
- 业务异常 code 校验
```

CI 可选命令：

```bash
newman run docs/postman/project.postman_collection.json \
  -e docs/postman/project.local.postman_environment.json
```

---

## 15. Maven / Gradle 执行规则

Maven：

```bash
mvn -f apps/server/pom.xml test
mvn -f apps/server/pom.xml verify
```

Gradle：

```bash
./gradlew test
./gradlew integrationTest
./gradlew check
```

如果项目尚未区分单元测试和集成测试，可以使用 Maven Surefire / Failsafe：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*ControllerTest.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IT.java</exclude>
                    <exclude>**/*ApiIT.java</exclude>
                </excludes>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>
                    <include>**/*ApiIT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## 16. 测试编写工作流

### 16.1 先检查

```text
是否已有同类测试？
是否已有 testsupport？
是否已有 TestDataFactory？
是否已有清理机制？
是否已有接口响应格式断言？
```

### 16.2 再决定测试类型

```text
业务逻辑 -> *Test
Controller 参数 / JSON -> *ControllerTest
Mapper / SQL -> *MapperIT
Redis 行为 -> *CacheIT
完整 Spring 链路 -> *IT
RANDOM_PORT API 合同 -> *ApiIT
```

### 16.3 再写测试

```text
先准备数据
再执行行为
最后断言 HTTP / DB / Redis / 副作用
```

### 16.4 最后运行测试

```bash
mvn -f apps/server/pom.xml test
mvn -f apps/server/pom.xml verify
```

---

## 17. 断言规则

### 17.1 单元测试断言

```text
断言返回值
断言异常类型
断言业务错误码
verify 关键依赖调用
verify 不应发生的调用 never()
```

### 17.2 Controller 测试断言

```text
HTTP status
JSON path
错误码
错误消息
参数校验字段
```

### 17.3 MapperIT 断言

```text
数据数据库写入
分页 total
排序顺序
逻辑删除状态
唯一约束异常
乐观锁 version
```

### 17.4 集成测试断言

```text
HTTP 响应
MySQL 最终状态
Redis 最终状态
事务回滚
权限拦截
```

### 17.5 ApiIT 断言

```text
HTTP status
响应 JSON
认证 header
业务错误码
MySQL 最终状态
Redis 最终状态
```

---

## 18. 禁止事项

```text
不允许用 H2 替代 MySQL 8 测 MyBatis-Plus SQL。
不允许连接开发库、共享测试库、本机 Redis。
不允许所有测试都写成 @SpringBootTest。
不允许所有接口都写成 RANDOM_PORT。
不允许只断言 HTTP 200，不断言响应体和数据库 / Redis 状态。
不允许依赖测试执行顺序。
不允许在测试里 Thread.sleep 作为主要同步手段。
不允许为了通过测试删除断言或禁用测试。
不允许 mock 掉本来应该集成验证的 Mapper、Redis、MySQL。
不允许在单元测试里启动 Spring Context。
不允许在共享环境执行 TRUNCATE 全库或 Redis FLUSHDB。
不允许把业务表清理逻辑写进 project-test-support。
```

---

## 19. 必须优先复用的开源模式

### 19.1 MyBatis-Plus 官方测试支持

用途：

```text
@MybatisPlusTest
mybatis-plus-spring-boot3-starter-test
Mapper / SQL 测试
```

参考：

```text
https://baomidou.com/en/getting-started/test/
https://github.com/baomidou/mybatis-plus
https://github.com/baomidou/mybatis-plus-samples
```

---

### 19.2 Spring Boot / Spring Framework 官方测试文档

用途：

```text
Testcontainers
@SpringBootTest
RANDOM_PORT
MockMvc
@Sql
TestExecutionListener
```

参考：

```text
https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
https://docs.spring.io/spring-framework/reference/testing/mockmvc.html
https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/executing-sql.html
https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/TestExecutionListener.html
```

---

### 19.3 Gradle / Maven 共享测试代码官方模式

用途：

```text
多模块共享测试基础设施
Gradle java-test-fixtures
Maven project-test-support / test-jar
```

参考：

```text
https://docs.gradle.org/current/javadoc/org/gradle/api/plugins/JavaTestFixturesPlugin.html
https://maven.apache.org/plugins/maven-jar-plugin/examples/create-test-jar.html
https://maven.apache.org/guides/mini/guide-attached-tests.html
```

---

### 19.4 Spring Security Test

用途：

```text
独立测试支持模块
测试依赖只用于 test scope
MockMvc 安全测试
```

参考：

```text
https://docs.spring.io/spring-security/reference/servlet/test/index.html
https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/setup.html
```

该项目证明了一个成熟生态也会把测试支持作为独立 test dependency 提供。  
所以多模块业务项目中创建 `project-test-support` 并不污染项目，前提是它只被 test scope 依赖。

---

### 19.5 Testcontainers Spring Boot Quickstart

用途：

```text
@SpringBootTest(RANDOM_PORT)
RestAssured
Testcontainers 数据库
@BeforeEach 清理数据
RANDOM_PORT API 测试模板
```

参考：

```text
https://github.com/testcontainers/testcontainers-java-spring-boot-quickstart
https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/
```

---

### 19.6 Spring PetClinic

用途：

```text
@WebMvcTest Controller 切片测试
Validator 单元测试
@SpringBootTest(RANDOM_PORT) RANDOM_PORT 测试
MySQL Testcontainers 示例
```

参考：

```text
https://github.com/spring-projects/spring-petclinic
https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java
https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/owner/PetValidatorTests.java
https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/PetClinicIntegrationTests.java
https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/MySqlIntegrationTests.java
```

---

### 19.7 JHipster Sample App

用途：

```text
统一 @IntegrationTest
MockMvc + Repository 断言
@Transactional 回滚
企业 CRUD REST 集成测试结构
```

参考：

```text
https://github.com/jhipster/jhipster-sample-app
https://github.com/jhipster/jhipster-sample-app/blob/main/src/test/java/io/github/jhipster/sample/IntegrationTest.java
https://github.com/jhipster/jhipster-sample-app/blob/main/src/test/java/io/github/jhipster/sample/web/rest/OperationResourceIT.java
```

---

### 19.8 Gtomika Spring Boot Testing Demo

用途：

```text
Service 单元测试 mock Repository
integrationTest 独立 source set
Testcontainers + MockMvc
CI 分阶段执行 unit / integration
```

参考：

```text
https://github.com/Gtomika/spring-boot-testing-demo
https://github.com/Gtomika/spring-boot-testing-demo/blob/master/src/test/java/com/epam/gaspar/securitydemo/service/DataServiceTest.java
https://github.com/Gtomika/spring-boot-testing-demo/blob/master/src/integrationTest/java/com/epam/gaspar/securitydemo/TestcontainersSetup.java
https://github.com/Gtomika/spring-boot-testing-demo/blob/master/build.gradle
```

---

### 19.9 不作为测试规范参考的项目

以下类型项目不作为本规范的测试体系参考来源：

```text
只有演示性质的 JUnit 样例，没有完整单元/集成/接口测试分层。
没有 Testcontainers 或等价容器化依赖隔离方案。
没有稳定 CI 测试链路。
测试目录只包含少量示例类，不能证明测试体系已落地。
```

`RuoYi-Vue-Plus` 不作为本规范的测试体系参考来源。  
它可以作为后台管理业务结构或脚手架风格参考，但不作为 Spring Boot 3 + MySQL 8 + Redis 8 + MyBatis-Plus 测试规范的依据。

---

## 20. 最小交付标准

### 20.1 新增 Service 逻辑

必须有：

```text
*ServiceTest
正常路径
至少一个异常路径
关键 Mapper 调用 verify
```

### 20.2 新增 Controller 接口

必须有：

```text
*ControllerTest 或 *ResourceIT
参数校验失败
成功响应 JSON
业务异常响应
```

### 20.3 新增 Mapper / XML SQL

必须有：

```text
*MapperIT
MySQL 8
正常查询
边界条件
空结果
分页或排序，如果涉及
```

### 20.4 新增 Redis 缓存

必须有：

```text
*CacheIT
写缓存
读缓存
失效缓存
TTL 或 key 结构断言
```

### 20.5 新增核心接口链路

必须有：

```text
*ApiIT
RANDOM_PORT API
认证/鉴权
核心成功流程
至少一个错误流程
默认 BASELINE_RESET
共享环境或并行环境使用 NAMESPACE_RESET
显式清理 MySQL/Redis
基础数据 seed 可重复
```

---

## 21. 当测试失败时的处理规则

测试运行失败后，按顺序排查：

```text
1. 先看是否是测试断言错误。
2. 再看是否是测试数据清理策略错误，例如 ApiIT 没有走 BASELINE_RESET。
3. 再看是否是 MySQL 表结构 / migration 未执行。
4. 再看是否是 Redis 未清理或 key prefix 不一致。
5. 再看是否是基础数据 seed 缺失或重复。
6. 再看是否是 Spring Context 配置问题。
7. 再看是否是 Docker / Testcontainers 不可用。
8. 最后才考虑修改生产代码。
```

禁止：

```text
为了让测试通过而降低断言强度。
删除关键断言。
把集成测试改成 mock 测试。
把 MySQL 改成 H2。
```

---

## 22. 最终执行要求

在本项目中编写、修改或评审测试时，必须遵循以下规则：

```text
1. 先读现有测试风格，再新增测试。
2. 优先使用项目已有测试工具类。
3. 不要引入与项目技术栈不一致的测试框架。
4. 单元测试不启动 Spring。
5. Mapper / SQL 测试必须使用 MySQL 8。
6. Redis 行为测试必须使用 Redis 8 容器。
7. 大量业务集成测试使用 MockMvc，不使用 RANDOM_PORT。
8. RANDOM_PORT ApiIT 只覆盖核心链路。
9. RANDOM_PORT ApiIT 默认使用 BASELINE_RESET 恢复测试初始化状态。
10. 共享环境或并行测试必须使用 NAMESPACE_RESET，禁止全库清理。
11. 涉及数据库和缓存的测试必须保证数据隔离。
12. 公共测试基础设施不写业务表、业务行、业务场景清理逻辑。
13. 模块级业务数据构造和 namespace 清理放当前模块 test support。
14. 应用级基础 seed 放启动模块 test support 或 app-test-support。
15. 修改接口时同步考虑 Postman / Newman。
16. 修改测试基础设施时保持向后兼容。
17. 测试要有业务意义，不要只追求覆盖率数字。
```
