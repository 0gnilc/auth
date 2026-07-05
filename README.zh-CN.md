# Gnilc Auth

[English](README.md)

Gnilc Auth 是一个面向 Java/Spring 应用的认证与授权框架，提供 RBAC 访问控制能力。项目采用 Maven 多模块结构，提供核心授权抽象、可选 Servlet 认证支持以及面向 RBAC 的实现。

项目 Maven `groupId` 与 Java 主包名统一使用 `com.gnilc.auth`，因为项目同时包含认证和授权能力。主包名下继续按职责区分：`com.gnilc.auth.authn.*` 表示认证，`com.gnilc.auth.authz.*` 表示授权，`com.gnilc.auth.system.*` 表示系统后台管理模块，用于编排认证、授权和 RBAC 资源。`com.gnilc.auth.system.auth.*` 放置系统后台管理 auth adapter，用于后台管理员会话认证和系统访问拒绝响应。

## 模块

- `gnilc-auth-core`：访问控制核心注解、决策接口、权限提供者、可选 Servlet 认证过滤器支持和 Servlet 授权过滤器支持。
- `gnilc-auth-rbac`：RBAC 相关缓存、实体、控制器、服务和权限提供者。
- `gnilc-auth-system`：系统应用模块，用于接入并初始化访问控制能力。

## 授权核心

`authz` 由两个功能模块组成：授权和权限校验。权限校验从 `AccessDecision` 开始；它只判断已授予权限是否满足所需权限。授权负责围绕本次决策准备访问事实和权限集合。

第一层包含授权与权限校验核心 module：`AccessDecision`、`GrantedPermissionsProvider`、`RequiredPermissionsProvider`、`AccessContext`（`AccessEnvironment`、`AccessIdentity`、`AccessTarget`）、`Permission`、`AccessDenied` 和 `AccessDeniedHandler`。`AccessDenied` 是决策后的全局访问拒绝入口，`AccessDeniedHandler` 是默认 implementation 可使用的有序策略；二者都不参与权限校验。

第二层包含 adapter/helper seam：`AccessContextAdapter`、`AccessEnvironmentResolver`、`AccessIdentityResolver` 和 `AccessTargetResolver`。`AccessContextAdapter` 是执行环境进入 authz 的主 seam；环境、身份和目标 resolver 是 adapter 内部可组合的 helper seam，不是强依赖。

第三层包含两个互不强依赖的功能模块：负责准备访问事实并调用 `AccessDecision` 的环境入口 implementation，以及把 `AccessContext` 映射为权限集合的 concrete `GrantedPermissionsProvider` / `RequiredPermissionsProvider` implementation。它们依赖核心 interface，而不依赖彼此的 implementation。

`AccessDenied` 通过 `denied(AccessContext, AccessDeniedContext)` 执行 `AccessDecision` 返回 false 后的访问拒绝。`AccessDeniedContext` 承载执行环境拒绝数据，与授权事实 `AccessContext` 分离。默认 implementation 会收集有序的 `AccessDeniedHandler` 策略，并调用所有 `supports(context, deniedContext)` 返回 true 的 handler。没有支持者时按 no-op 处理。

## 可选 Servlet 认证

应用可以通过定义一个或多个 `ServletAuthenticationHandler` Bean 启用认证过滤器。没有 handler bean 时，认证过滤器不会注册。

多个 handler 按 Spring order 排序。不支持当前请求的 handler 会被跳过；如果没有任何 handler 支持当前请求，请求链继续执行，由授权规则决定匿名访问是否允许。认证失败会停止请求链，默认返回 401。授权失败仍保持独立，继续由现有访问拒绝处理路径负责。

## 响应体业务码

`R.code` 是业务响应码，不是 HTTP Status。HTTP 状态应从 transport response 读取，业务结果应从 JSON 响应体读取。当前业务码区间为：`0` 表示成功，`10000-19999` 表示通用业务/请求错误，`20000-29999` 表示认证、会话和授权错误。

## 环境要求

- JDK 17+
- Maven 3.8+

## 构建

```bash
mvn clean package
```

## 提交规范

提交信息应遵循项目规范：[.github/commit-convention.md](.github/commit-convention.md)。
