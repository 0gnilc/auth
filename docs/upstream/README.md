# Vue Vben Admin 上游同步记录

本目录用于记录当前项目对 [`vbenjs/vue-vben-admin`](https://github.com/vbenjs/vue-vben-admin) 上游提交的评估与同步结果。

当前项目基于 Vue Vben Admin 改造，但只保留 `admin` 与 `server` 两个核心应用。上游的缺陷修复、性能优化和工程改进不会自动进入当前项目，需要逐个提交评估后决定是否同步。

本目录是项目维护文档，不参与 npm 包版本发布，也不要求业务功能或 Java 变更创建记录。每个同步记录只对应一个上游 Git commit。

## 当前同步周期

当前周期的唯一汇总入口是 [`2026-07-29-sync-plan.md`](./2026-07-29-sync-plan.md)。该文档按实际执行状态列出从固定基线到目标版本的全部 25 个 first-parent 提交；状态数量和提交清单不在其他文档中重复维护。

总览中的状态表示当前项目的实际处理结果，不表示最初的同步建议。每个上游提交只能出现在一个状态分组中；具体差异、过滤原因和验证结果以对应的单提交台账为准。

部分同步必须明确区分：

- **已同步内容**：已经进入当前项目的变更。
- **待后续同步内容**：仍适用于当前项目，但因依赖关系或批次边界暂未实施。
- **永久过滤内容**：已评估并决定不进入当前项目的上游应用、示例、文档或无关依赖。

## 覆盖范围

优先评估上游提交对以下目录的影响：

- `apps/admin`
- `packages`
- `internal`
- `scripts`
- 根目录中的前端工程配置

`apps/server` 是本项目独立维护的 Spring Boot 模块，除非上游提交涉及共享的 monorepo 工具链，否则不属于同步范围。

## 记录规则

1. 每个上游 commit 创建一个 `<12位commit-sha>.md` 文件。
2. 从 [`template.md`](./template.md) 复制字段，并填写完整的 40 位上游 SHA。
3. 一个文件只记录一个上游 commit，不把多个提交合并成一条记录。
4. 状态为 `synced` 或 `partial` 时，必须填写本项目对应的 `local_commits`。
5. 状态为 `skipped` 或 `not-applicable` 时，必须写明原因。
6. 状态为 `partial` 时，必须分别记录已同步、待后续同步和永久过滤内容；没有对应内容时明确填写“无”。
7. 同步完成后保留记录，不删除文件，以便后续审计和避免重复评估。
8. `local_commits` 不能填写记录文件自身所在的 commit；应先提交代码修改，再用后续 commit 更新记录，两者放在同一个 PR。

## 状态定义

| 状态 | 含义 |
| --- | --- |
| `baseline` | 当前项目的上游派生起点，不表示该提交之前的所有实现均被原样保留 |
| `pending` | 已纳入当前周期，但尚未同步；可处于待评估或已确认等待实施状态 |
| `reviewing` | 正在分析影响范围或验证方案 |
| `synced` | 已完整同步到当前项目 |
| `partial` | 已同步部分内容，仍有待后续同步或永久过滤的内容 |
| `skipped` | 与当前项目有关，但因已有等价实现或明确保留本地方案而决定不同步 |
| `not-applicable` | 不适用于当前项目结构或已删除的上游应用 |

## 建议流程

首次使用时，为当前仓库配置 Vue Vben Admin 上游 remote：

```bash
git remote add upstream https://github.com/vbenjs/vue-vben-admin.git
```

如果 `upstream` 已存在，不要重复添加。每次检查前先获取上游 `main` 的最新提交：

```bash
git fetch upstream main
```

然后按以下步骤处理：

1. 确认 `upstream/main` 已更新。
2. 始终从固定的 `baseline` SHA 获取 `upstream/main` 的 first-parent 提交列表，不使用“最近登记的 SHA”作为扫描起点：

   ```bash
   git rev-list --first-parent --reverse <baseline-sha>..upstream/main
   ```

3. 将完整提交列表与本目录所有记录中的 `upstream_commit` 字段比较，为每个缺失 SHA 创建记录，初始状态设为 `pending`。
4. 检查提交涉及的文件是否仍存在于当前项目，以及本地代码是否已经偏离上游实现。
5. 根据实际情况同步、部分同步或跳过，并记录原因和验证结果。
6. 需要修改代码时，先提交代码修改，再在后续 ledger commit 中填写 `local_commits`；代码与记录应放在同一个 PR。

采用 first-parent 历史是为了跟踪实际进入上游 `main` 的提交序列，避免把尚未合并分支上的中间提交误当成同步目标。

上游提交列表：<https://github.com/vbenjs/vue-vben-admin/commits/main/>
