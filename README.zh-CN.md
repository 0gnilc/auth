# Access Control

[English](README.md)

Access Control 是一个基于 Java/Spring 的访问控制项目。项目采用 Maven 多模块结构，提供核心授权抽象以及面向 RBAC 的实现。

当前 README 仅用于项目初始化阶段的简要介绍，后续会继续补充更完整的使用说明和设计文档。

## 模块

- `access-control-core`：访问控制核心注解、决策接口、权限提供者和 Web 过滤器支持。
- `access-control-rbac`：RBAC 相关缓存、实体、控制器、服务和权限提供者。
- `access-control-example`：用于演示项目使用方式的示例模块。

## 环境要求

- JDK 17+
- Maven 3.8+

## 构建

```bash
mvn clean package
```

## 提交规范

提交信息应遵循项目规范：[.github/commit-convention.md](.github/commit-convention.md)。
