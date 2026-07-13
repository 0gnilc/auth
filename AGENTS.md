## Agent skills

### Issue tracker

Issues and PRDs are tracked in this repo's GitHub Issues via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Triage uses the default canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Testing

Before writing, changing, reviewing, or running tests, read and follow [`docs/test/testing-guide.md`](docs/test/testing-guide.md). Java tests belong under the owning Maven module's `src/test`; do not use `src/intg-test`. Node engineering-tool tests are the documented exception and belong under the owning package's `scripts/test`.

Run fast Surefire tests (`*Test` and `*ControllerTest`) with:

```bash
mvn -f apps/server/pom.xml test
```

Run the complete Surefire and Failsafe suite (`*IT`, `*MapperIT`, `*CacheIT`, and `*ApiIT`) with:

```bash
mvn -f apps/server/pom.xml verify
```

`mvn -f apps/server/pom.xml verify` requires Docker for Testcontainers MySQL 8 and Redis 8. Do not replace containers with H2, local services, or shared services.

### Domain docs

This repo uses a single-context domain docs layout: root `CONTEXT.md` plus root `docs/adr/`. See `docs/agents/domain.md`.
