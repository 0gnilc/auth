# Access Control

[English](README.md)

Access Control 是一个基于 Java/Spring 的访问控制项目。项目采用 Maven 多模块结构，提供核心授权抽象、可选 Servlet 认证支持以及面向 RBAC 的实现。

项目 Maven `groupId` 是 `com.gnilc.auth`，因为项目同时包含认证和授权能力。Java 包名继续按职责区分：`com.gnilc.authn.*` 表示认证，`com.gnilc.authz.*` 表示授权。这不是对现有授权 API 的 Java 包整体迁移。

## 模块

- `access-control-core`：访问控制核心注解、决策接口、权限提供者、可选 Servlet 认证过滤器支持和 Web 授权过滤器支持。
- `access-control-rbac`：RBAC 相关缓存、实体、控制器、服务和权限提供者。
- `access-control-example`：用于演示项目使用方式的示例模块。

## 可选 Servlet 认证

应用可以通过定义一个或多个 `AuthenticationHandler` Bean 启用认证过滤器。没有 handler bean 时，认证过滤器不会注册。

多个 handler 按 Spring order 排序。不支持当前请求的 handler 会被跳过；如果没有任何 handler 支持当前请求，请求链继续执行，由授权规则决定匿名访问是否允许。认证失败会停止请求链，默认返回 401。授权失败仍保持独立，继续由现有访问拒绝处理路径负责。

## 环境要求

- JDK 17+
- Maven 3.8+

## 构建

```bash
mvn clean package
```

## 提交规范

提交信息应遵循项目规范：[.github/commit-convention.md](.github/commit-convention.md)。
