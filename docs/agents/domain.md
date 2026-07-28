# Domain Documentation

This repository uses multiple domain contexts. The root `CONTEXT.md` is their index; module `CONTEXT.md` files own their respective vocabularies. File location alone does not make any of them automatic agent input.

## Reading Order

Before exploring or changing domain behavior:

1. Apply instructions from the root and relevant nested `AGENTS.md` files.
2. Read the root [`CONTEXT.md`](../../CONTEXT.md).
3. Read every module `CONTEXT.md` mapped to the behavior being changed.
4. Read the relevant ADRs in the root [`docs/adr/`](../adr/).

When a task crosses contexts, read each context instead of treating one glossary as a global override.

## Layout

```text
/
├── CONTEXT.md                 # context index and relationships
├── docs/adr/                 # all architectural decisions
├── apps/admin/
│   └── CONTEXT.md            # Admin System language
└── apps/server/
    └── CONTEXT.md            # Server authentication and authorization language
```

## Content Boundaries

- `CONTEXT.md` defines stable domain terms in one or two sentences. It contains no package layout, class responsibilities, framework rules, API contracts, or implementation recipes.
- `AGENTS.md` records verifiable implementation and workflow instructions for agents.
- ADRs record durable, costly-to-reverse decisions and their rationale. Keep every ADR in the root `docs/adr/`, including decisions scoped to one context.
- Do not copy the same rule or definition into multiple files. Link to its canonical owner.

## Vocabulary And Conflicts

Use the canonical term from the relevant context in code, tests, issues, and design notes. If a requested change conflicts with an ADR or uses a term differently from its glossary definition, surface the conflict before silently changing the model.
