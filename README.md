# Access Control

[中文文档](README.zh-CN.md)

Access Control is a Java/Spring-based access control project. It is organized as a multi-module Maven project and provides core authorization abstractions, optional Servlet authentication support, and an RBAC-oriented implementation.

The Maven `groupId` is `com.gnilc.auth` because the project contains both authentication and authorization capabilities. Java packages remain purpose-specific: `com.gnilc.authn.*` is authentication, and `com.gnilc.authz.*` is authorization. This is not a package-wide rename of existing authorization APIs.

## Modules

- `access-control-core`: core access control annotations, decision interfaces, permission providers, optional Servlet authentication filter support, and web authorization filter support.
- `access-control-rbac`: RBAC-related cache, entities, controllers, services, and permission providers.
- `access-control-system`: system application module for bootstrapping access control integration.

## Optional Servlet authentication

Applications can opt in to the authentication filter by defining one or more `AuthenticationHandler` beans. If no handler bean exists, the authentication filter is not registered.

Multiple handlers are ordered by Spring order. A handler that does not support the current request is skipped; if no handler supports the request, the filter chain continues and authorization decides whether anonymous access is allowed. A failed authentication stops the chain and returns 401 by default. Authorization failures remain separate and are handled by the existing access-denied path.

## Requirements

- JDK 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

## Commit convention

Commit messages should follow the project convention in [.github/commit-convention.md](.github/commit-convention.md).
