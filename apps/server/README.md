# gnilc-auth Server

Spring Boot 3 / Java 17 application with reusable authentication and RBAC modules, administrator identity, Redis sessions, and dynamic internationalization.

- `gnilc-bootstrap`: `com.gnilc.bootstrap.AuthBootApplication` and profiles.
- `gnilc-core`: application adapters under `com.gnilc.core`.
- `gnilc-auth-core` / `gnilc-auth-rbac`: application-neutral authentication and authorization.
- `gnilc-common`: shared infrastructure and test support.

Use the root [setup guide](../../README.zh-CN.md), [SQL initialization](deploy/sql/README.md), and [test strategy](../../docs/test/test-strategy.md).

```bash
pnpm --filter=@app/server dev
pnpm test:server
pnpm verify:server
pnpm build:server
```
