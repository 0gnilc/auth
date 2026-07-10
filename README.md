# Gnilc Auth

[中文文档](README.zh-CN.md)

Gnilc Auth is a Java/Spring authentication and authorization framework with RBAC-based access control support. It is organized as a multi-module Maven project and provides core authorization abstractions, optional Servlet authentication support, and an RBAC-oriented implementation.

Packages remain purpose-specific: `com.gnilc.auth.authn.*` is authentication, `com.gnilc.auth.authz.*` is authorization, and `com.gnilc.system.*` is the system administration module that coordinates authentication, authorization, and RBAC resources. `com.gnilc.system.auth.*` contains the system-administration auth adapters for admin-session authentication and system access-denied responses.

## Modules

- `gnilc-common`: parent/aggregator for common modules.
- `gnilc-common/gnilc-test-support` (`gnilc-test-support`): behavior-neutral shared test containers, cleanup, and test utilities.
- `gnilc-auth`: parent/aggregator for authentication and authorization modules.
- `gnilc-auth/auth-core` (`auth-core`): core access-control annotations, decisions, permission providers, and optional Servlet authentication/authorization adapters.
- `gnilc-auth/auth-rbac` (`auth-rbac`): RBAC entities, mappers, services, controllers, permission providers, and cache behavior.
- `gnilc-system`: administrator profiles, sessions, and system-level auth composition.
- `gnilc-bootstrap`: executable application and whole-application integration boundary.

## Authorization core

`authz` is composed of two functional modules: authorization and permission checking. Permission checking starts at `AccessDecision`; it only decides whether granted permissions satisfy required permissions. Authorization prepares the access facts and permission sets around that decision.

The first layer contains the core authz modules: `AccessDecision`, `GrantedPermissionsProvider`, `RequiredPermissionsProvider`, `AccessContext` (`AccessEnvironment`, `AccessIdentity`, `AccessTarget`), `Permission`, `AccessDenied`, and `AccessDeniedHandler`. `AccessDenied` is the global post-decision denied entry, and `AccessDeniedHandler` is an ordered strategy used by the default implementation; neither participates in permission checking.

The second layer contains adapter/helper seams: `AccessContextAdapter`, `AccessEnvironmentResolver`, `AccessIdentityResolver`, and `AccessTargetResolver`. `AccessContextAdapter` is the main seam from an execution environment into authz. The environment, identity, and target resolvers are optional helper seams composed inside an adapter; they are not strong dependencies.

The third layer contains two independent functional modules: environment-entry implementations that prepare access facts and invoke `AccessDecision`, and concrete `GrantedPermissionsProvider` / `RequiredPermissionsProvider` implementations that map an `AccessContext` to permissions. They depend on the core interfaces, not on each other's implementations.

`AccessDenied` executes the denied path after `AccessDecision` returns false through `denied(AccessContext, AccessDeniedContext)`. `AccessDeniedContext` carries execution-environment denial data separately from authorization facts. The default implementation collects ordered `AccessDeniedHandler` strategies and invokes every handler whose `supports(context, deniedContext)` returns true. If no handler supports the denied context, the default implementation is a no-op.

## Optional Servlet authentication

Applications can opt in to the authentication filter by defining one or more `ServletAuthenticationHandler` beans. If no handler bean exists, the authentication filter is not registered.

Multiple handlers are ordered by Spring order. A handler that does not support the current request is skipped; if no handler supports the request, the filter chain continues and authorization decides whether anonymous access is allowed. A failed authentication stops the chain and returns 401 by default. Authorization failures remain separate and are handled by the existing access-denied path.

## Response body codes

`R.code` is a business response code, not an HTTP status code. Read HTTP status from the transport response, and read business result from the JSON body. The current body-code ranges are: `0` for success, `10000-19999` for common business/request failures, and `20000-29999` for authentication, session, and authorization failures.

## Requirements

- JDK 17+
- Maven 3.8+

## Build and test

```bash
# Fast Surefire tests: *Test and *ControllerTest
mvn test

# Full verification: Surefire plus Failsafe *IT/*MapperIT/*CacheIT/*ApiIT
# Docker is required for Testcontainers MySQL 8 and Redis 8.
mvn verify

# Build the reactor after verification
mvn clean package
```

All tests live under the owning module's `src/test`; there is no `src/intg-test` source set. Integration tests use disposable Testcontainers services, never H2, local services, or shared services. See the mandatory [testing guide](docs/test/testing-guide.md) for naming, module selection, cleanup, and HTTP assertion policy.

## Commit convention

Commit messages should follow the project convention in [.github/commit-convention.md](.github/commit-convention.md).
