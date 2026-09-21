# 数据库初始化

本项目面向专用的新 MySQL 8 数据库 `gnilc_auth`。在同一个 MySQL 连接中按文件名顺序执行（首次管理授权使用会话内的初始化标记）：

1. `01_rbac.sql`：授权主体、角色、权限、菜单与关联表。
2. `02_admin.sql`：管理员、默认 `admin` 账号与基础访问角色。
3. `03_framework_permissions.sql`：框架端点权限。
4. `04_rbac_permissions.sql`：RBAC 管理权限。
5. `05_admin_permissions.sql`：管理员会话、自助与管理权限。
6. `06_i18n.sql`：动态国际化及相关角色、菜单。
7. `07_rbac_admin.sql`：内置资源保护、后台管理菜单和授权绑定。

```bash
cat apps/server/deploy/sql/*.sql | mysql --default-character-set=utf8mb4 -u root -p gnilc_auth
```

开发默认账号为 `admin` / `123456`。重复初始化保留已有管理员密码和资料；新库首次初始化授予 RBAC 和国际化管理角色。默认基础角色由脚本恢复，普通管理角色不因重复初始化强制恢复。

`sys_admin.avatar` 保存可选头像 URL，NULL 表示未设置。本项目不包含对象存储表或业务平台表。

这些脚本不用于将业务平台存量数据库升级为本项目。请创建独立数据库和 Redis 环境；后续已部署版本的结构变化需要专门的迁移脚本，不要依赖 `CREATE TABLE IF NOT EXISTS` 更新已有列。

模块将 SQL 复制到测试 classpath 的 `sql/schema/`。`pnpm verify:server` 使用 Testcontainers 验证 schema、重复初始化、真实 MySQL/Redis 行为以及所有应用 API 的权限注册，测试不连接开发数据库。
