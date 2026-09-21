# 测试策略

`pnpm test` 运行工作区和 Admin 的 Vitest 测试，以及 Server 的 Maven `test`。快速 Server 测试不依赖 Docker。

`pnpm verify:server` 运行 Maven `verify`，增加 `*IT` 集成测试。测试使用 Testcontainers 提供的一次性 MySQL 8.4 和 Redis 8；数据清理受 test profile、显式开关和测试数据库名约束。

`pnpm test:e2e` 预留独立的 Admin 和 Server 端口，通过 `E2eServerIT` 启动完整应用，再运行 Playwright Chromium。已有开发服务不被关闭或复用。

`pnpm verify` 包含服务端完整验证和浏览器测试。浏览器测试首次运行需安装 Chromium：`pnpm exec playwright install chromium`。

覆盖重点：认证和授权分流、会话刷新与撤销、基础角色保护、RBAC 管理与国际化权限矩阵、缓存失效、管理员资料清空语义、部署 SQL 幂等性和启动组合。启动器测试验证端口竞争、启动失败、信号转发与资源回收。

CI 根据改动范围选择工作区、Admin、Server 和浏览器任务。Server schema、资源、配置、DAO、缓存与集成测试变更触发完整服务端验证；公共 API 边界变更同时触发浏览器验证。最终 CI gate 校验所有已选择任务的实际结果。

发布验收使用 `pnpm release:verify`，固定源码提交并完整运行 workspace、类型、E2E、Admin 构建及 Maven clean verify，不使用按路径裁剪的发布测试。未提交代码使用 `pnpm release:verify --local` 演练，产物不可正式发布。详细操作见 [版本管理与交付维护指南](../release/README.md)。
