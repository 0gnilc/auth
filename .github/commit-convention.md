# Git Commit Convention

This project follows Conventional Commits and references the [vue-vben-admin commit convention](https://github.com/vbenjs/vue-vben-admin/blob/main/.github/commit-convention.md). The convention is enforced by the local `commit-msg` hook and by the GitHub repository ruleset.

## Commit format

```text
<type>(<scope>): <subject>
```

`scope` is optional:

```text
<type>: <subject>
```

A full commit message may include a body and footer:

```text
<header>

<body>

<footer>
```

## Type

Only the following types are allowed:

| Type | Description | Changelog |
| --- | --- | --- |
| `feat` | New feature | Yes |
| `fix` | Bug fix | Yes |
| `perf` | Performance improvement | Yes |
| `docs` | Documentation change | No |
| `style` | Formatting or style change that does not affect logic | No |
| `refactor` | Refactoring without adding features or fixing bugs | No |
| `test` | Test-related change | No |
| `workflow` | Workflow-related change | No |
| `build` | Build system or dependency change | No |
| `ci` | CI configuration change | No |
| `chore` | Miscellaneous maintenance change | No |
| `types` | Type definition change | No |
| `wip` | Work-in-progress commit | No |
| `revert` | Revert commit | No |

Any commit that contains `BREAKING CHANGE:` is treated as a breaking change and should be included in the changelog, regardless of its type.

## Scope

`scope` identifies the module or area affected by the change. Use short lowercase names, for example:

- `core`
- `rbac`
- `example`
- `build`
- `ci`
- `docs`
- `workflow`

Scoped package names such as `@app/server` and `@vben-core/shared` are also valid.

## Subject

- Required.
- Use imperative, present-tense wording.
- Start with a lowercase letter.
- Do not end with a period.
- Keep it concise. The local and remote rules limit the header to 100 characters.

## Body

Use the body to explain:

- Why the change is needed.
- How the new behavior differs from the previous behavior.
- Important implementation infos or migration notes.

## Footer

Use the footer for breaking changes and issue references:

```text
BREAKING CHANGE: describe the breaking change
```

```text
close #28
```

## Revert commits

A revert commit should use:

```text
revert: <reverted commit header>

This reverts commit <hash>.
```

## Examples

```text
feat(core): add permission evaluator
```

```text
fix(rbac): resolve role inheritance lookup

close #12
```

```text
perf(build): cache dependency resolution

BREAKING CHANGE: build cache key format has changed.
```

```text
revert: feat(core): add permission evaluator

This reverts commit abc1234.
```
