# Gnilc Auth Context

## 授权核心

### 授权（Authorization）

授权是围绕一次访问事实准备权限集合并调用权限校验的过程。授权负责准备访问环境、访问身份、访问目标、已授予权限和所需权限；它不把执行环境对象直接交给权限校验 module。

### 权限校验（Permission Checking）

权限校验判断已授予权限是否满足所需权限。对应 module 命名为 `AccessDecision`；它只执行 allow/deny 判断，不解析权限来源、不构造访问上下文、不处理拒绝结果。

### Authz 三层分层

第一层是授权与权限校验核心 module：`AccessDecision`、`GrantedPermissionsProvider`、`RequiredPermissionsProvider`、`AccessContext`（包含 `AccessEnvironment`、`AccessIdentity`、`AccessTarget`）、`Permission`、`AccessDenied` 和 `AccessDeniedHandler`。其中 `AccessDecision` 负责权限校验，其余 module 围绕授权事实、权限集合和拒绝处理展开。

第二层是 adapter/helper seam：`AccessContextAdapter`、`AccessEnvironmentResolver`、`AccessIdentityResolver` 和 `AccessTargetResolver`。`AccessContextAdapter` 是执行环境对象进入 authz 的主 seam；`AccessEnvironmentResolver`、`AccessIdentityResolver` 与 `AccessTargetResolver` 是 adapter implementation 内部的 helper seam，分别负责提取访问环境、访问身份和访问目标。四者是组合关系，不做强依赖；只要能构造完整 `AccessContext`，使用方也可以不使用 helper resolver。

Servlet 授权命名遵循两层语言：`Web*` 用于功能/配置入口，`Servlet*` 用于依赖 Jakarta Servlet API 的 concrete adapter、filter 和 denied context。默认 Servlet adapter 组合 `DefaultServletAccessIdentityResolverHandler` 与 `ServletAccessTargetResolver`；默认 identity handler 作为 Servlet handler 链最后兜底规则，将认证产生的 `AccessPrincipal` 转换为授权使用的 `AccessIdentity`。完整执行环境入口仍是 `ServletAccessContextAdapter`。Servlet request、response 和 filter chain 只能出现在 `ServletAccessDeniedContext` 等拒绝执行上下文中，不能进入 `AccessContext`。

第三层包含两个互不强依赖的功能模块：环境入口抽象实现 module 与 provider 授权实现 module。环境入口抽象实现 module 负责授权前准备，组合当前环境的 `AccessContextAdapter` 与全局 `AccessDecision`；provider 授权实现 module 负责 concrete `GrantedPermissionsProvider` / `RequiredPermissionsProvider` implementation。两者都只依赖核心授权事实和 provider interface，不关心对方的 implementation。

### 认证（Authentication）

认证是确认一次访问由哪个登录主体发起的过程。认证成功后会产生可用于授权的访问身份；认证本身不判断该身份是否有权访问目标。

### 已授予权限（Granted Permission）

已授予权限是访问主体在一次授权判断中可使用的权限集合。它可以来自用户角色、用户组、系统身份、任务身份、临时授权或匿名默认权限。

对应 module 命名为 `GrantedPermissionsProvider`，职责是根据一次访问事实提供已授予权限。

### 所需权限（Required Permission）

所需权限是访问目标在一次授权判断中要求满足的权限集合。它可以来自 Servlet 路由、消息主题、任务入口、业务访问点或其他受保护访问点。

对应 module 命名为 `RequiredPermissionsProvider`，职责是根据一次访问事实提供所需权限。

### 访问上下文（Access Context）

访问上下文是一次授权判断的核心访问事实输入，包含访问环境、访问身份、访问目标和补充属性。访问环境是 provider 判断是否参与本次授权判断的一等事实；当前默认预置 Servlet 访问环境，其他环境可通过访问环境标识扩展。补充属性不承担环境隔离职责。访问上下文不包含 Servlet request、数据库连接、缓存或其他执行环境对象；这些对象应由 adapter 翻译成访问上下文。

### 访问环境（Access Environment）

访问环境标识一次授权判断所属的执行环境。当前默认预置 `AccessEnvironment.SERVLET`；其他环境可通过 `AccessEnvironment.of(String)` 扩展，而不是在 core 中预置常量。`AccessEnvironmentResolver` 可以作为 adapter 内部 helper seam 提取访问环境。`GrantedPermissionsProvider` 与 `RequiredPermissionsProvider` 应根据访问环境判断是否参与当前授权判断，避免不同环境的同名身份或同名权限被合并到一次决策中。

### 访问身份（Access Identity）

访问身份是一次访问的身份事实，可以表示用户、匿名访问者、系统身份、任务身份、服务账号或外部调用方。`AccessIdentityResolver` 可以作为 adapter 内部 helper seam 提取访问身份；`GrantedPermissionsProvider` 通常根据访问身份和访问上下文提供已授予权限。

### 访问目标（Access Target）

访问目标是一次访问指向的受保护目标，可以表示 Servlet 路由、消息主题、任务入口、业务访问点或领域对象。访问目标可以包含可选限定符，用来区分同一目标标识下的不同受保护变体，例如 HTTP method、消息消费/发布方向或任务操作。`AccessTargetResolver` 可以作为 adapter 内部 helper seam 提取访问目标。

### 授权决策（Access Decision）

授权决策是权限校验 module 的入口，判断已授予权限是否满足所需权限。`AccessDecision` 不解析已授予权限、不解析所需权限、不处理拒绝结果，只执行决策规则。

### 访问拒绝处理（Access Denied Handling）

访问拒绝处理是在授权决策失败后，由当前执行环境执行的处理动作。Servlet 场景可以返回 403、写响应或抛异常；消息、任务或命令行场景可以采用各自的拒绝处理方式。

`AccessDenied` 是访问拒绝的全局唯一入口 module，发生在 `AccessDecision` deny 之后，通过 `denied(AccessContext, AccessDeniedContext)` 执行访问拒绝。`AccessDeniedContext` 表示执行环境拒绝上下文，与只承载授权事实的 `AccessContext` 分离。`AccessDeniedHandler` 是默认 implementation 可使用的策略 interface，可以有多个 implementation；默认 `AccessDenied` implementation 会按 Spring order 调用所有支持当前 `AccessContext` 和 `AccessDeniedContext` 的 handler。没有支持者时按 no-op 处理。`AccessDenied` 与 `AccessDeniedHandler` 都不参与权限校验。

## 后台管理

### 后台管理员用户（Admin User）

后台管理员用户是后台管理系统中可登录的人员身份，拥有可维护的账号资料，例如昵称等展示信息。每个后台管理员用户对应一个 RBAC 授权主体，并通过该授权主体参与角色、权限和菜单授权。

### 后台管理员登录凭据（Admin Credentials）

后台管理员登录凭据是后台管理员用户用于证明其身份的用户名与密码组合。登录凭据只用于认证，不等同于认证成功后签发的后台管理员会话或其中的令牌。

### 后台管理员会话（Admin Session）

后台管理员会话是后台管理员用户认证成功后形成的登录会话事实，包含访问令牌、刷新令牌及其配对关系。会话模块负责签发、校验、刷新、撤销和清理后台管理员会话；令牌格式、Redis key 组织、TTL、刷新令牌与访问令牌配对规则属于该模块的实现细节，不应泄漏到 Controller 或后台管理员用户资料维护流程中。

### 默认管理员基线（Default Admin Baseline）

默认管理员基线是系统完成初始化后必须存在的内置后台管理员用户、对应的 RBAC 授权主体、内置管理员角色及其必要绑定。恢复基线只恢复系统身份及必要关系，不覆盖密码、昵称等可维护的管理员资料。

### 系统 Auth 组合包（System Auth Composition）

`com.gnilc.system.auth.*` 是后台管理系统 auth 组合包，放置后台管理员会话认证和系统访问拒绝响应等系统专属 concrete adapter。它属于 `com.gnilc.system.*` 的编排能力，不属于纯 `com.gnilc.auth.authn.*` 或纯 `com.gnilc.auth.authz.*` 核心包。

## 认证与授权边界

项目 Maven `groupId` 与 Java 主包名统一使用 `com.gnilc.auth`，表示项目覆盖认证与授权能力。主包名下继续按职责区分：`com.gnilc.auth.authn.*` 表示认证（authentication），`com.gnilc.auth.authz.*` 表示授权（authorization）。`com.gnilc.system.*` 表示后台管理系统能力，它可以编排认证、授权和 RBAC 资源，但不归属于纯认证或纯授权包。

认证负责验证凭证并形成当前执行环境可读取的主体事实；授权负责基于访问身份、访问目标和权限集合做允许/拒绝判断。认证失败通常由认证失败处理返回 401；授权失败由访问拒绝处理返回 403 或执行环境约定的拒绝动作。

Servlet 认证过滤器是可选 Servlet 适配能力。只有应用提供 `ServletAuthenticationHandler` Bean 时才注册认证过滤器；没有处理器支持当前请求时，请求继续进入后续链路，由授权规则决定匿名访问是否允许。

## 响应体业务码

`R.code` 是 JSON 响应体中的业务响应码，不是 HTTP Status。HTTP Status 只由 transport 层表达，例如 `ResponseEntity` 或 `HttpServletResponse`；业务调用方不能把 `R.code` 当作 HTTP Status 使用。

当前业务码区间：`0` 表示成功；`10000-19999` 表示通用业务/请求错误；`20000-29999` 表示认证、会话和授权错误。当前内置映射为：`ERROR=10000`、`ARGUMENT_INVALID=10001`、`ILLEGAL_CONDITION=10002`、`AUTHENTICATION_FAILED=20001`、`UNAUTHORIZED=20002`、`ACCESS_DENIED=20003`。
