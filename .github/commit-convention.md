# Git Commit Convention

本项目提交信息遵循 Conventional Commits 风格，并参考 [vue-vben-admin commit convention](https://github.com/vbenjs/vue-vben-admin/blob/main/.github/commit-convention.md)。本规范同时通过本地 `commit-msg` hook 和 GitHub 仓库 ruleset 进行约束。

## 提交格式

```text
<type>(<scope>): <subject>
```

`scope` 可选：

```text
<type>: <subject>
```

完整提交可包含正文和页脚：

```text
<header>

<body>

<footer>
```

## 类型（type）

仅允许以下类型：

| 类型 | 说明 | 是否进入变更日志 |
| --- | --- | --- |
| `feat` | 新功能 | 是 |
| `fix` | 问题修复 | 是 |
| `perf` | 性能优化 | 是 |
| `docs` | 文档变更 | 否 |
| `style` | 代码格式、样式调整，不影响逻辑 | 否 |
| `refactor` | 重构，不新增功能也不修复缺陷 | 否 |
| `test` | 测试相关 | 否 |
| `workflow` | 工作流相关 | 否 |
| `build` | 构建系统或依赖相关 | 否 |
| `ci` | CI 配置相关 | 否 |
| `chore` | 杂项维护 | 否 |
| `types` | 类型定义相关 | 否 |
| `wip` | 临时开发提交 | 否 |
| `revert` | 回滚提交 | 否 |

包含 `BREAKING CHANGE:` 的提交，无论类型如何，都视为破坏性变更并应进入变更日志。

## 范围（scope）

`scope` 用于说明变更影响的模块或区域。建议使用小写短词，例如：

- `core`
- `rbac`
- `example`
- `build`
- `ci`
- `docs`
- `workflow`

## 主题（subject）

- 必填。
- 使用祈使句、现在时。
- 以小写字母开头。
- 不以句号结尾。
- 建议简洁明确；本地与远程规则限制 header 最长 100 个字符。

## 正文（body）

正文用于解释：

- 为什么需要该变更。
- 新行为与旧行为的差异。
- 重要实现细节或迁移说明。

## 页脚（footer）

页脚用于记录破坏性变更和 issue 关联：

```text
BREAKING CHANGE: describe the breaking change
```

```text
close #28
```

## 回滚提交

回滚提交应使用：

```text
revert: <reverted commit header>

This reverts commit <hash>.
```

## 示例

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
