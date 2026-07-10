# 部署 SQL 说明

本目录保存人工执行的部署和初始化 SQL。应用和自动化测试不会自动执行这些脚本。

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

## 自动化测试

自动化集成测试只使用 Testcontainers 创建的临时 MySQL 8 schema `gnilc_auth_test`。测试负责创建并销毁该环境；不要让自动化测试连接 `access`、`access_local_it` 或其他本地/共享 schema。

## 人工本地验证

如需人工执行部署 SQL，只能使用专用 schema `access_local_it`，并明确传入连接参数。不要把数据库密码写入脚本、命令历史、日志或文档。示例仅展示执行顺序：

```bash
mysql --host=127.0.0.1 --user=<user> --password --database=access_local_it \
  < deploy/sql/01-rbac.sql
mysql --host=127.0.0.1 --user=<user> --password --database=access_local_it \
  < deploy/sql/02-admin.sql
```

执行前确认当前 schema：

```sql
SELECT DATABASE();
```

安全约束：

- 不要写入 `application-dev.yml` 默认业务库 `access`。
- `access_local_it` 仅用于人工本地验证；它不是自动化测试依赖。
- 不要对共享数据库运行清理或初始化脚本。
- 本流程只处理 MySQL SQL，不涉及 Redis。
