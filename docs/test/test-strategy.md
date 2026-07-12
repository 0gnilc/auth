# Test strategy

This repository uses one test source set per owning Maven module. Test names select
the execution lane:

| Suffix | Scope | Maven lane |
| --- | --- | --- |
| `*Test`, `*ControllerTest` | Unit, focused auto-configuration, and HTTP controller contracts | Surefire, `mvn test` |
| `*MapperIT` | MyBatis-Plus mappings and MySQL behavior | Failsafe, `mvn verify` |
| `*CacheIT`, `*IT` | Spring integration and Redis behavior | Failsafe, `mvn verify` |
| `*ApiIT` | Random-port HTTP flows | Failsafe, `mvn verify` |

## Infrastructure

`gnilc-test-support` contains only business-neutral test infrastructure:

- JVM-scoped MySQL 8.4 and Redis 8 containers;
- Spring context property initializers;
- guarded database and Redis cleanup;
- API baseline reset orchestration;
- RestAssured random-port setup.

The deployment scripts under `deploy/sql` are the only schema input. They are
copied to the test classpath by Maven and initialized in the temporary MySQL
database. Each owning module declares which deployment scripts and MyBatis
properties its tests need; the shared support module contains no RBAC or admin
schema knowledge. Module tests never use H2, a local database, or a local Redis
service.

## Isolation

Mapper and service integration tests use Spring transaction rollback. Redis tests
flush the isolated container database after each method. Random-port API tests
cannot rely on test transactions, so `@ApiTest` performs this lifecycle:

1. verify the active `test` profile, cleanup flag, database name, actual JDBC
   endpoint, and actual Redis host, port, and database against the running
   Testcontainers instances;
2. flush Redis and truncate all business tables;
3. run application-owned `BaselineDataSeeder` beans;
4. flush Redis again;
5. execute the test;
6. flush Redis and truncate business tables after the test.

The bootstrap module owns the application baseline. It replays the real admin
deployment seed and adds the protected-path permissions, menu, and limited-role
account needed by API test flows.

## Commands

```bash
mvn test
mvn verify
```

`mvn test` must remain Docker-free. `mvn verify` requires Docker and fails
instead of substituting a different database or cache.
