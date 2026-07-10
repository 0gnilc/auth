# Testing policy

This is the mandatory repository-specific policy for writing, reviewing, and running tests. Keep tests deterministic, behavior-focused, and owned by the module whose behavior they verify. A test case ID is not required.

## Layout, names, and Maven phases

All tests and test resources live under the owning module's `src/test`; do not create or restore an `src/intg-test` source set.

| Suffix | Purpose | Maven plugin and command |
| --- | --- | --- |
| `*Test` | Unit, service, utility, or focused Spring test | Surefire's configured `**/*Test.java` include: `mvn test` |
| `*ControllerTest` | Controller contract, usually MockMvc; also matches `*Test` | Surefire: `mvn test` |
| `*IT` | Spring/component integration | Failsafe's configured `**/*IT.java` include: `mvn verify` |
| `*MapperIT` | MyBatis/SQL behavior against MySQL; also matches `*IT` | Failsafe: `mvn verify` |
| `*CacheIT` | Redis behavior; also matches `*IT` | Failsafe: `mvn verify` |
| `*ApiIT` | Random-port HTTP flow; also matches `*IT` | Failsafe: `mvn verify` |

`mvn test` is the fast lane and must not start Testcontainers. `mvn verify` runs both the fast lane and Failsafe integration tests; Docker is required. If Docker is unavailable, report that integration verification could not run—never substitute H2 or shared infrastructure.

Useful selections:

```bash
# All fast tests
mvn test

# All tests, including integration tests (Docker required)
mvn verify

# One module and its reactor dependencies
mvn -pl :auth-core -am test
mvn -pl :gnilc-system -am verify

# One Surefire class; -am also passes the selector to upstream modules
mvn -pl :auth-rbac -am \
  -Dtest=RbacPermissionProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

# One Failsafe class; tolerate upstream modules without that class
mvn -pl :gnilc-bootstrap -am \
  -Dit.test=AuthorizationApiIT \
  -Dfailsafe.failIfNoSpecifiedTests=false verify
```

Use the module paths shown by the root Maven reactor. Do not use `-DskipTests` when claiming verification.

## Infrastructure and isolation

Tests that exercise persistence or caching use Testcontainers only. Current defaults are `mysql:8.4.0` with disposable schema `gnilc_auth_test`, and `redis:8-alpine` using database 0; container reuse is disabled.

- real MySQL 8 for Mapper, schema, SQL, and database-backed integration behavior;
- real Redis 8 for cache, token, session, TTL, and eviction behavior.

H2, local MySQL/Redis, development databases, and shared services are prohibited. Mocks are appropriate for unit boundaries, but a mock is not evidence of MySQL or Redis behavior.

Destructive database or Redis cleanup must pass `TestEnvironmentGuard` first. It requires the `test` profile plus `app.test.cleanup.enabled=true` and `app.test.container.owned=true`. MySQL cleanup additionally requires catalog `gnilc_auth_test` and an exact owned-container host/port match; Redis requires the same exact host/port ownership match. A failed check must throw and refuse cleanup. Never weaken the guard to make a test pass.

Choose the smallest cleanup mode that isolates the behavior:

| Mode | Use |
| --- | --- |
| `NONE` | Unit and controller-slice tests with no persistent state |
| `TRANSACTION_ROLLBACK` | Same-thread database tests whose writes are covered by the Spring test transaction |
| `REDIS_CLEAN` | Redis tests, or transactional database tests with Redis side effects; clean before and after |
| `BASELINE_RESET` | Random-port, asynchronous, or cross-thread flows; reset MySQL and Redis, then restore required baseline data |

A random-port request runs outside the test method's transaction, so `*ApiIT` must not rely on `@Transactional` rollback. Use `BASELINE_RESET` before each case and a best-effort cleanup afterward. Full-table truncation and Redis database flush are allowed only against the guarded, test-owned containers.

## Ownership and selection matrix

| Change or question | Owning module | Preferred test |
| --- | --- | --- |
| Authentication/authorization abstractions, Servlet filters and adapters | `gnilc-auth/auth-core` | `*Test`; focused `*ControllerTest` when HTTP contract is involved |
| RBAC services, controllers, mappers, permission cache | `gnilc-auth/auth-rbac` | Service `*Test`, `*ControllerTest`, `*MapperIT`, or `*CacheIT` according to the boundary |
| Shared generic test infrastructure | `gnilc-common/gnilc-test-support` | `*Test`; consuming module integration tests prove wiring |
| Admin profiles, sessions, and system composition | `gnilc-system` | Service `*Test`, `*ControllerTest`, `*MapperIT`, or `*CacheIT` |
| Whole-application startup and public HTTP flows | `gnilc-bootstrap` | `*IT` or a small number of high-value `*ApiIT` cases |

Keep business fixtures, seeders, and assertions in the owning module's `src/test`. Only behavior-neutral container and cleanup mechanisms belong in `gnilc-test-support`. `IntegrationTest` supplies the `test` profile but does not itself add `@SpringBootTest`; choose and declare the required Spring test surface. `ApiTest` supplies random-port Spring Boot, full-stack containers, `BASELINE_RESET`, and the reset listener. Do not move all tests to `gnilc-bootstrap` merely because it can start the application.

## Pick the lightest test surface

| Need | Test surface |
| --- | --- |
| Business branch, value object, token format, or collaborator orchestration | Plain JUnit 5 with mocks; no Spring context |
| Route, binding, JSON, exception mapping, or HTTP contract with mocked collaborators | `@WebMvcTest`/MockMvc in `*ControllerTest` |
| Mapper, wrapper, logical delete, index, paging, or SQL behavior | `*MapperIT` with Testcontainers MySQL 8 |
| Redis key/value, TTL, session, eviction, or reset behavior | `*CacheIT` with Testcontainers Redis 8 |
| Multiple real Spring components without a real network port | `*IT`, usually MockMvc, with only the required containers |
| A critical end-to-end HTTP contract across real threads | `*ApiIT` with `RANDOM_PORT`, the required containers, and `BASELINE_RESET` |

Do not use `@SpringBootTest` for logic that a plain unit test can prove. Do not use random port for routine controller branches.

## HTTP and auth contracts

Assert transport status, response content type/body, and business result independently.

For protected system endpoints, the current externally observable distinction is intentional:

- no access token means authorization sees an anonymous request and responds **HTTP 403** with JSON `R.code=20003`;
- a Bearer token beginning with the `sys_admin.` namespace but malformed or invalid is handled as authentication failure and responds **HTTP 401**, content type `text/plain;charset=UTF-8`, body `invalid access token`.

A credential not claimed by the `sys_admin.` handler (for example `Bearer garbage` or `Basic ...`) continues as anonymous and may therefore receive the protected endpoint's JSON 403. Use the actual `Authorization: Bearer ...` contract and namespace when constructing the 401 case.

`R.code` is a JSON business code, not an HTTP status. Business failures can use HTTP 200 with a nonzero `R.code`; transport/system failures use their real HTTP status. Tests must not infer one from the other. Assert the authorization 403 as JSON, but do not assert the plain-text authentication 401 as an `R` response.

## Definition of done

A change is test-complete when:

- tests live under the owning module's `src/test` and follow the configured suffix;
- the lightest suitable surface covers the changed external behavior and important failure path;
- MySQL/Redis behavior uses guarded Testcontainers MySQL 8/Redis 8 and the correct cleanup mode;
- tests do not depend on order, local state, shared services, fixed container ports, or sleeps;
- HTTP tests assert status separately from body and `R.code`;
- `mvn test` passes for fast changes;
- `mvn verify` passes for integration-affecting changes and was run with Docker;
- CI's `fast-tests` and `full-verification` jobs pass, with reports uploaded when a job fails;
- changed setup or behavior is reflected in repository documentation.

## Troubleshooting

1. Read `target/surefire-reports` for `*Test`/`*ControllerTest` failures and `target/failsafe-reports` for integration failures; CI uploads these directories on failure.
2. Confirm the class suffix selected the expected Maven plugin and that the test is under `src/test`.
3. For `mvn verify`, confirm Docker is reachable and inspect Testcontainers startup logs. Do not fall back to H2 or local services.
4. Confirm the test profile, dynamic MySQL/Redis properties, and cleanup guard point to the test-owned containers.
5. Check the cleanup mode: transactions do not roll back random-port or asynchronous work; Redis always needs explicit cleanup.
6. Check SQL/bootstrap order and baseline seed assumptions, then look for leaked Redis keys or rows from the preceding case.
7. Reproduce with a module/test selection command before changing assertions or production code. Never disable a test or weaken an assertion only to make the build green.
