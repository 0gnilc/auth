# Access Control Context

## 授权核心

### 已授予权限（Granted Permission）

已授予权限是访问主体在一次授权判断中可使用的权限集合。它可以来自用户角色、用户组、系统身份、任务身份、临时授权或匿名默认权限。

对应模块命名为 `GrantedPermissionsProvider`，职责是根据一次访问事实提供已授予权限。

### 所需权限（Required Permission）

所需权限是访问目标在一次授权判断中要求满足的权限集合。它可以来自 Web 路由、消息主题、任务入口、业务访问点或其他受保护访问点。

对应模块命名为 `RequiredPermissionsProvider`，职责是根据一次访问事实提供所需权限。

### 访问上下文（Access Context）

访问上下文是一次授权判断的环境无关输入，包含访问身份、访问目标和补充属性。它不包含 Servlet request、MQ message、数据库连接、缓存或其他执行环境对象；这些对象应由 adapter 翻译成访问上下文。

### 访问身份（Access Identity）

访问身份是一次访问的身份事实，可以表示用户、匿名访问者、系统身份、任务身份、服务账号或外部调用方。`GrantedPermissionsProvider` 通常根据访问身份和访问上下文提供已授予权限。

### 访问目标（Access Target）

访问目标是一次访问指向的受保护目标，可以表示 Web 路由、消息主题、任务入口、业务访问点或领域对象。访问目标可以包含可选限定符，用来区分同一目标标识下的不同受保护变体，例如 HTTP method、消息消费/发布方向或任务操作。

### 授权决策（Access Decision）

授权决策判断已授予权限是否满足所需权限。`AccessDecision` 不解析已授予权限、不解析所需权限、不处理拒绝结果，只执行决策规则。

### 访问拒绝处理（Access Denied Handling）

访问拒绝处理是在授权决策失败后，由当前执行环境执行的处理动作。Web 场景可以返回 403、写响应或抛异常；消息、任务或命令行场景可以采用各自的拒绝处理方式。

推荐模块命名为 `AccessDeniedHandler`，职责是处理拒绝结果，不参与授权判断。

## 认证与授权边界

项目 Maven `groupId` 使用 `com.gnilc.auth`，表示项目覆盖认证与授权能力。Java 包名按职责继续区分：`com.gnilc.authn.*` 表示认证（authentication），`com.gnilc.authz.*` 表示授权（authorization）。

认证负责验证凭证并形成当前执行环境可读取的主体事实；授权负责基于访问身份、访问目标和权限集合做允许/拒绝判断。认证失败通常由认证失败处理返回 401；授权失败由访问拒绝处理返回 403 或执行环境约定的拒绝动作。

Servlet 认证过滤器是可选 Web 适配能力。只有应用提供 `AuthenticationHandler` Bean 时才注册认证过滤器；没有处理器支持当前请求时，请求继续进入后续链路，由授权规则决定匿名访问是否允许。
