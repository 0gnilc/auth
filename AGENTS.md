# Working With This Repository

Choose implementation and verification methods using your judgment. Challenge outdated code, documentation, and proposed designs, explain better alternatives, and keep implementation within the requested scope. Explain the impact of changes to business behavior or published contracts.

## Project And References

- This repository is a pnpm/Turborepo workspace with an Admin Vue application and a Spring Boot/Maven Server.
- The root instructions apply repository-wide; module `AGENTS.md` files contain local additions. Keep these files in English and explain work to the user in Chinese.
- [CONTEXT.md](CONTEXT.md) introduces the business model. The [ADR index](docs/adr/README.md) records current decisions and historical alternatives. Consult relevant material as needed.

## Code Preferences

- Vue components use `<script setup lang="ts">`. Prefer inferred TypeScript types inside typed boundaries.
- For new or changed semantic classes in project-owned Vue UI, use kebab-case BEM: `block`, `block__element`, `block--modifier`. Existing upstream classes and utility or third-party tokens keep their conventions.
- Keep user-facing text in the owning localization resources.
- Write project-owned explanatory comments and database `COMMENT` text in Chinese, retaining necessary technical terms and identifiers. Comments explain non-obvious reasons and constraints; important contracts must not exist only in chat history.
- Document every business type and field with Javadoc/JSDoc. Keep simple field descriptions brief; explain units, precision, time zones, null semantics, and snapshots where relevant.
- Document inherited fields at their declaration, and synchronize related comments and database `COMMENT` text when business contracts change.
- Workspace dependency versions come from the root catalog; formatting and lint configuration own mechanical style.

## Agent Coding Rules

Goal: Meet current requirements while maintaining project consistency and human maintainability. Do not optimize for the fewest tokens, lines of code, or files.

### 1. Investigate as Needed; Do Not Guess from Partial Context

- Read project conventions, relevant implementations, callers, and tests according to the scope of the change. Inspect the corresponding mechanisms when configuration, exception handling, or cross-module relationships are involved. Do not read the entire repository or create a lengthy plan for a small change.
- Before adding or replacing functionality, start with relevant implementations and choose search clues as needed, such as responsibilities, synonyms, types, or call sites. Expand to shared modules and existing dependencies only when missing information affects the current decision. Stop when there is enough information to support reuse or a local implementation; do not exhaustively search the repository. If nothing is found, state the search scope rather than claiming that no implementation exists.
- Decide routine naming and local decomposition independently. Ask questions only about unresolved ambiguities that affect business behavior, security, or public contracts.

### 2. Reuse Before Deciding Whether to Abstract

- Read candidate implementations and compare business semantics, exceptions, side effects, and dependency directions. Reuse compatible implementations without creating reverse dependencies or business coupling solely for reuse. Explain differences when an implementation is incompatible.
- When no suitable implementation is found, implement the current requirement directly. Base abstractions on duplicated logic with the same semantics, explicit public contracts, necessary dependency isolation, or existing architecture. Do not add interfaces, factories, strategies, or configuration options for speculative extensions.
- Before extracting shared functionality, confirm that callers share the same rule and should change together, and determine where the functionality belongs. Do not abstract or inline mechanically based on usage counts. Use neutral names for business-independent functionality; preserve business meaning in domain rules and keep them out of generic `Utils`.

### 3. Evidence-Based Validation and Contract-Based Configuration

- Validate at boundaries involving untrusted input or external systems. Before removing an internal check, confirm that the same condition is already guaranteed by types, entry-point validation, or storage constraints across the relevant call paths, and identify the supporting evidence.
- Authorization, tenant isolation, and business-state checks are not format validation; do not remove them merely because a call is internal. Enforce each rule in its responsible layer rather than duplicating it across layers.
- Validate required configuration through existing mechanisms at startup or when the relevant feature initializes; invalid configuration must prevent that initialization. Use only contract-permitted defaults for optional configuration, without adding ad hoc fallbacks at business call sites. Trimming and case conversion require a basis in the format rules. Do not modify keys, passwords, or raw signing input without an explicit contract, and do not supply weak default keys.

### 4. Expose Failures; Do Not Disguise Them as Success

- Distinguish legitimate absence of results, business rejections, configuration errors, and system failures. Follow existing exception, `Optional`, or `Result` conventions rather than introducing another framework. Null values, empty collections, and similar values may represent legitimate outcomes, but must not conceal failures.
- Catch exceptions only for recovery, necessary translation, resource cleanup, or reporting at an entry point, and preserve the cause. Do not use empty `catch` blocks or pointless catch-and-rethrow code. Do not assume centralized exception handling covers every entry point.
- On runtime failure, terminate the current operation according to its contract rather than always exiting the process. When changing exception handling, retries, or fallbacks, check transaction rollback, side effects, and idempotency semantics. Do not add retry or fallback mechanisms without an explicit requirement.

### 5. Keep Source Code Maintainable by Humans

- Make the main flow clearly express inputs, business decisions, side effects, and outputs. Names should convey business meaning, units, or state. Avoid hard-to-read abbreviations, nested ternaries, and compact expressions that mix side effects.
- Use helper methods to express meaningful steps or isolate complexity. Avoid pointless wrappers and indirection while preserving useful layers and responsibility boundaries.
- Limit final cleanup to the current changes, preserve required behavior, and address only concrete, identifiable problems rather than repeatedly switching between equivalent forms. Do not manually rewrite generated or minified artifacts for readability.

### 6. Verification and Feedback

- Choose suitable verification methods based on the task, scope of impact, and risk, while following project requirements. When checks need to run, use the project's actual commands rather than applying a fixed checklist.
- Reuse verification results that still apply to the current code, environment, and relevant assumptions. Avoid repeating checks without a clear verification purpose. Do not weaken tests or bypass checks just to obtain a passing result.
- Provide the conclusions, supporting evidence, and unresolved issues needed for the current task. When verification is involved, report results and limitations honestly; do not present inferences or unexecuted checks as verified facts.

## Development

- `pnpm dev` starts the full stack, selects available ports, and connects frontend proxies. Leave existing services running; use transient port overrides for individual launches.
- `pnpm test` covers workspace, Admin, and fast Server tests; `pnpm verify` adds Server infrastructure tests and full-stack browser tests. Other focused commands are in `package.json`.
- Issues and PRDs live in GitHub Issues, accessed through `gh`; [triage labels](docs/agents/triage-labels.md) describe the project-specific workflow.

## Delivery

- Leave changes uncommitted until the user requests a commit. Use `pnpm run commit` for commits.
- Push and PR creation or updates require an explicit remote-delivery request. Deliver through a PR, defaulting its base to `main`; never push directly to the target branch.
