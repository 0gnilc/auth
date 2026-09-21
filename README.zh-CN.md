# gnilc-auth

基于 Vue 3 / Vben / Element Plus 与 Spring Boot 3 的管理员认证与授权项目。

保留管理员登录、刷新、注销、个人资料和密码修改、管理员管理、RBAC 角色/权限/菜单管理及动态国际化。认证、授权、拒绝处理、Redis 会话和缓存采用当前项目实现。

## 项目结构

- `apps/admin`：后台应用。
- `apps/server/gnilc-bootstrap`：启动入口与环境配置。
- `apps/server/gnilc-core`：管理员身份、会话、访问适配、动态国际化与通用工具。
- `apps/server/gnilc-auth`：与应用无关的认证与 RBAC 模块。
- `apps/server/gnilc-common`：公共基础设施和测试支持。
- `packages`、`internal`：后台使用的 Vben 组件和构建工具。

## 开发

需要 Node.js 22.18+ 或 24、pnpm 11、Java 17、Maven、MySQL 8 和 Redis。

```bash
pnpm install
pnpm dev
```

启动前创建专用数据库 `gnilc_auth`，按 [SQL 初始化说明](apps/server/deploy/sql/README.md) 初始化。通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 配置数据库，通过 `SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT`、`SPRING_DATA_REDIS_DATABASE` 配置 Redis。生产配置另要求 `SPRING_DATA_REDIS_PASSWORD`。开发配置中的密码占位符需要替换或通过环境变量覆盖。

默认开发端口：Server 3888，Admin 5088；`pnpm dev` 自动选择可用端口并同步前端代理。初始化的开发管理员为 `admin` / `123456`，生产使用前修改密码并分配所需管理角色。

## 验证与构建

```bash
pnpm test                 # 工作区、Admin 和 Server 快速测试
pnpm check:type           # 前后端类型/编译检查
pnpm verify               # 隔离 MySQL/Redis 集成测试与浏览器测试，需要 Docker
pnpm build:admin
pnpm build:server
```

服务端可执行 JAR 位于 `apps/server/gnilc-bootstrap/target/gnilc-bootstrap-<版本>-exec.jar`，版本取自根 `package.json`。生产启动时指定 `--spring.profiles.active=prod`。Admin 生产构建默认请求同源 `/api`；示例 Nginx 将其转发到同一容器网络中的 `server:3888`，部署时需要提供该服务名或调整上游地址。

## 提取后的契约

这是面向新数据库的独立项目，不提供业务平台存量数据库的原地迁移。只初始化认证、授权、管理员和国际化数据；旧业务迁移和业务账号不属于本项目。

管理员头像统一使用可选 `avatar` URL，数据库仍使用 `sys_admin.avatar`；不再返回或接收对象存储字段。空值及资料更新的省略/清空语义保持不变。应用不需要对象存储、支付渠道或 IP 数据库配置。

[领域术语](CONTEXT.md) · [角色设计](docs/role/role-design.md) · [测试策略](docs/test/test-strategy.md) · [架构决策](docs/adr/README.md)

[版本管理与发布流程](docs/release/README.md) · [变更日志](CHANGELOG.md)
