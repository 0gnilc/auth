# Testing policy

This is the mandatory repository-specific policy for writing, reviewing, and running tests. Keep tests deterministic, behavior-focused, and owned by the module whose behavior they verify. A test case ID is not required.

## Layout, names, and Maven phases

All test-owned code, fixtures, and resources live under the owning module's `src/test`; do not create or restore an `src/intg-test` source set. Production deployment SQL remains under root `deploy/sql` and may be copied to the test classpath as a shared input so integration tests can verify the real deployment schema. Module-owned test initializers decide which deployment scripts their tests execute.

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

## Test value gate

Tests do not map one-to-one to production classes or methods. A production type does not need a dedicated unit test merely because it exists. Before adding or retaining a test, answer all four questions:

1. **Interface:** Which caller-visible behavior, invariant, error mode, or infrastructure contract does this test protect?
2. **Failure:** What realistic regression can make this test fail while the implementation still compiles?
3. **Evidence:** What is the lightest test surface capable of proving that behavior?
4. **Authority:** Is this the authoritative test for that fact, or is the same fact already proved more effectively elsewhere?

Delete or do not create a test when it only:

- repeats getters, setters, constructors, enum constants, framework annotations, or constant values;
- mocks every collaborator and then verifies the implementation called those mocks in the same order as the source code;
- tests a pass-through/no-op implementation with no policy, branching, state, failure handling, or compatibility contract;
- reads configuration metadata as text or reflects on annotations when an application-context or adapter integration test already proves the wiring;
- reproduces a branch already covered at the same confidence level by another test;
- cannot name a plausible regression beyond "the code changed".

Keep a lower-level test alongside a higher-level test only when it adds different evidence, such as exhaustive policy branches, deterministic fault injection, concurrency behavior, or a precise failure diagnosis. Test count and line coverage are observations, not goals.

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

## Choose the test type by required evidence

Always start at the top of this table and move down only when the lighter surface cannot produce the required evidence.

| Required evidence | Test type | Use it for | Do not use it for |
| --- | --- | --- | --- |
| Deterministic policy, algorithm, invariant, parsing, state transition, concurrency, or failure orchestration | Plain `*Test` | Access decisions, token formats, cache state machines, ordered handlers, collaborator failures that are difficult to create with real infrastructure | Getters/setters, pass-through methods, framework metadata, SQL/Redis/HTTP behavior |
| Servlet route, request binding, JSON shape, status, content type, exception mapping, or filter response | Focused `*ControllerTest` with MockMvc | Controller contracts and transport branches with mocked business interfaces | Business implementation, database behavior, routine full application startup |
| Spring conditional wiring or bean backoff that is itself a published module contract | Focused `*Test` with `ApplicationContextRunner` | Optional filters, default beans, user-provided bean replacement, servlet/non-servlet conditions | Reading imports files as text, reflecting on annotations, proving the whole application starts |
| MyBatis mapping, generated SQL, constraints, logical delete, paging, or transaction semantics | `*MapperIT` with MySQL | Facts that can differ between MySQL and an in-memory model | Business branches already provable without a database |
| Redis commands, key layout, TTL, atomic replacement, Pub/Sub, or eviction semantics | `*CacheIT` with Redis | Facts that require a real Redis server | Cache-independent business orchestration |
| Several real Spring modules, transactions, events, AOP, database, or cache collaborating without a network port | Focused `*IT` | Module composition whose behavior depends on Spring or infrastructure semantics | A single deterministic class or a routine controller branch |
| A critical externally observable journey across real HTTP threads and the complete filter chain | `*ApiIT` with `RANDOM_PORT` | Login/session lifecycle, authorization outcomes, and a small number of high-value administration journeys | Exhaustive field validation, every CRUD branch, internal bean wiring |
| Application packaging and auto-configuration as a whole | Small startup `*IT` | Proving the assembled application starts and essential adapters are present | Exact lists of metadata entries or every bean in the context |

### Escalation rules

- Start with a plain test when the behavior is deterministic and independent of Spring or external infrastructure.
- Upgrade to a focused Spring test only when conditional configuration, serialization, validation, filters, transactions, events, or AOP are part of the behavior being proved.
- Upgrade to MySQL or Redis integration only when the real adapter can disagree with a mock or in-memory implementation.
- Upgrade to random-port HTTP only when thread boundaries, the complete filter chain, network serialization, or transport behavior are essential evidence.
- Do not use `@SpringBootTest` merely to avoid constructing dependencies. Do not use random port for routine controller branches.

### Avoid horizontal duplication

One behavior should have one authoritative test surface:

- plain tests own branch-heavy rules and injected failure paths;
- Mapper/Cache tests own database and Redis semantics;
- Controller tests own route, binding, JSON, and exception contracts;
- API tests own only critical cross-module journeys.

A higher-level test may repeat a small assertion as part of a journey, but it must not recreate every lower-level branch. Conversely, a unit test must not mirror a higher-level workflow with every dependency mocked. Prefer deleting the weaker duplicate over keeping both "for coverage".

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
- every new test passes the value gate and names the unique evidence it contributes;
- duplicated facts have one authoritative test surface, with weaker duplicates removed;
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
