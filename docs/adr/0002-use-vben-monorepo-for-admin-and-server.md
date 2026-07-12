# ADR 0002: Use the Vben monorepo for admin and server

## Status

Accepted

## Context

The repository previously contained only the Gnilc Auth Maven reactor. The product now needs an Element Plus administration application while retaining the existing Spring Boot server and root-level Agent guidance.

## Decision

Use Vue Vben Admin's pnpm and Turborepo structure as the repository shell. Keep two product applications:

- `apps/admin`, derived from the upstream `apps/web-ele` application;
- `apps/server`, containing the existing Maven reactor unchanged apart from its repository path.

Keep the Vben internal workspace packages required by the admin application. Exclude the other Vben UI variants, Nitro mock backend, documentation application, playground, mobile application, and contract-generation packages.

The server exposes Maven tasks through `apps/server/package.json` so Turbo can schedule both ecosystems without treating Java as a JavaScript project.

Root-level Agent and domain documentation remains authoritative for the whole monorepo.

## Consequences

- Node.js, pnpm, Java, and Maven are required for complete local development.
- Admin and server can be developed or built independently through root scripts.
- Vben workspace packages remain part of the repository because `apps/admin` depends on them.
- The admin request adapter still requires product-specific API integration work beyond the repository restructuring.
