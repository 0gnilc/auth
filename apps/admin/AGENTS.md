# Admin Application Instructions

## Domain

- Read [`CONTEXT.md`](CONTEXT.md) and the relevant ADRs in the root [`docs/adr/`](../../docs/adr/) before changing administrator, RBAC, menu, or dynamic internationalization behavior.
- The server owns authorization and business invariants. UI access-code checks control action visibility only.

## API Modules

- Define cohesive resource types and derive operation-specific inputs inline with TypeScript utilities such as `Pick` and `Omit`.
- Do not create one interface per API operation.
- Follow the organization used by [Vben Admin's system API modules](https://github.com/vbenjs/vue-vben-admin/tree/main/playground/src/api/system).

## UI Changes

- Inspect neighboring views and shared components before introducing a new pattern.
- Preserve unsaved-change protection for mutable drawers and forms.
- Keep Message Key persistence separate from the enclosing business-resource save, as defined by the relevant ADRs.
