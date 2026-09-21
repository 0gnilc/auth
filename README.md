# gnilc-auth

A standalone administrator authentication and authorization workspace built with Vue 3, Vben, Element Plus, Spring Boot 3, MySQL and Redis.

It includes administrator sessions, profiles and password changes, administrator management, RBAC roles/permissions/menus, and dynamic internationalization. The application uses the current authentication, authorization, rejection handling and cache implementations.

- `apps/admin`: administrator UI.
- `apps/server/gnilc-bootstrap`: executable application and profiles.
- `apps/server/gnilc-core`: administrator identity, sessions and application adapters.
- `apps/server/gnilc-auth`: reusable authentication and RBAC.
- `apps/server/gnilc-common`: shared infrastructure and test support.

See the [Chinese setup guide](README.zh-CN.md) for environment variables, database initialization, build and deployment details, and the extraction's API contract changes.

```bash
pnpm install
pnpm dev
pnpm test
pnpm check:type
pnpm verify
pnpm build:admin
pnpm build:server
```

Development requires Node.js 22.18+ or 24, pnpm 11, Java 17, Maven, MySQL 8 and Redis. Full verification uses disposable Docker containers. Initialize a fresh `gnilc_auth` database using the [SQL guide](apps/server/deploy/sql/README.md). Avatar values are optional URLs in the `avatar` field; object storage is not required.

[Domain glossary](CONTEXT.md) · [Architecture decisions](docs/adr/README.md) · [Testing](docs/test/test-strategy.md)
