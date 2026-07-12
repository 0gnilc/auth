## Summary

<!-- What changed, and why? Link the issue with `Closes #...` when applicable. -->

## Verification

<!-- List the commands run and their results. If not run, explain why. -->

- [ ] `mvn -f apps/server/pom.xml test`
- [ ] `mvn -f apps/server/pom.xml verify` (requires Docker)

## Checklist

- [ ] I followed [`docs/test/testing-guide.md`](../docs/test/testing-guide.md) for test placement, naming, isolation, and cleanup.
- [ ] New or changed behavior has the appropriate unit, controller, mapper, cache, integration, or API coverage.
- [ ] Integration tests use Testcontainers MySQL 8 and/or Redis 8; they do not use H2, local services, or shared services.
- [ ] Destructive cleanup is guarded and uses the correct cleanup mode.
- [ ] HTTP status and `R.code` are asserted separately where applicable.
- [ ] Documentation and SQL instructions are updated when behavior or setup changed.
