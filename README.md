# Access Control

[中文文档](README.zh-CN.md)

Access Control is a Java/Spring-based access control project. It is organized as a multi-module Maven project and provides core authorization abstractions plus an RBAC-oriented implementation.

This README is intentionally brief for the initial project setup. More detailed documentation will be added later.

## Modules

- `access-control-core`: core access control annotations, decision interfaces, permission providers, and web filter support.
- `access-control-rbac`: RBAC-related cache, entities, controllers, services, and permission providers.
- `access-control-example`: example module for demonstrating project usage.

## Requirements

- JDK 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

## Commit convention

Commit messages should follow the project convention in [.github/commit-convention.md](.github/commit-convention.md).
