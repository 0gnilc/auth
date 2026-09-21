# Server Development

Server uses Spring Boot, MyBatis-Plus, and Lombok. Prefer `@Data` for mutable DTOs/VOs and `@Value` for internal immutable carriers.

- Application APIs conventionally use POST with JSON DTOs; published endpoints retain their methods, including self-service GET endpoints.
- `R.code` is a JSON business code, independent of HTTP status.
- Stored and exposed enums use explicit protocol values so Java renaming does not alter persisted or JSON values.
- MyBatis-Plus normally skips null fields in updates. Fields cleared by a full update use `FieldStrategy.ALWAYS`.
- `gnilc-auth` is application-neutral; Application-specific identity and access adapters live under `com.gnilc.core`.
- Keep database `COMMENT` contracts synchronized with current-schema DDL. Future changes for an existing deployment require a dedicated migration and isolated MySQL verification.
- [Server test infrastructure](../../docs/test/test-strategy.md) explains Maven lanes and disposable MySQL/Redis services.
