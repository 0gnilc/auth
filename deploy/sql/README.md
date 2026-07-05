# 部署 SQL 说明

本目录保存部署和初始化相关 SQL。

## 文件说明

- `01-rbac.sql`
  - RBAC 模块当前表结构的空库 schema 脚本。
  - 用于干净 schema 首次创建 RBAC 表。

- `02-admin.sql`
  - 系统后台管理员初始化脚本。
  - 包含 `sys_admin` 表结构、默认管理员账号和 RBAC 绑定数据。
  - 依赖先执行 `01-rbac.sql`。
  - 该文件保留幂等判断，适合人工重复执行时避免覆盖已有数据。

## 迁移脚本说明

本目录不再保留旧 RBAC schema 到当前 schema 的迁移脚本。旧库升级应由使用方结合自身迁移系统和实际历史结构单独维护。

## 执行顺序

首次初始化干净 schema 时，按以下顺序人工执行：

1. `01-rbac.sql`
2. `02-admin.sql`

安全约束：

- 不要写入 `application-dev.yml` 默认业务库 `access`。
- 如需本地验证，使用专用 schema `access_local_it`。
- 不要在命令、日志或文档中写入数据库密码。
- 本流程只处理 MySQL SQL，不涉及 Redis。
