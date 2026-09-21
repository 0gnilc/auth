# gnilc-auth Server

基于 Spring Boot 3 / Java 17，提供管理员身份、Redis 会话、认证授权、RBAC 和动态国际化。

- `gnilc-bootstrap`：启动入口 `com.gnilc.bootstrap.AuthBootApplication` 与环境配置。
- `gnilc-core`：`com.gnilc.core` 下的管理员业务和应用适配。
- `gnilc-auth-core` / `gnilc-auth-rbac`：与应用无关的认证、授权和 RBAC。
- `gnilc-common`：公共基础设施和测试支持。

环境配置见根目录[安装说明](../../README.zh-CN.md)，新库配置见 [SQL 初始化说明](deploy/sql/README.md)，验证方法见[测试策略](../../docs/test/test-strategy.md)。

```bash
pnpm --filter=@app/server dev
pnpm test:server
pnpm verify:server
pnpm build:server
```
