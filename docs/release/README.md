# 版本维护指南

Gnilc Auth 使用统一产品版本。根 `package.json.version` 是唯一版本源，Admin、Server、内部 workspace 包和 Maven 模块同步升版；第三方依赖版本不随产品升版修改。

## 版本规则

| 变更 | 升级方式 | 示例 |
| --- | --- | --- |
| 兼容的缺陷或安全修复 | PATCH | `1.0.0 → 1.0.1` |
| 兼容的新功能 | MINOR | `1.0.1 → 1.1.0` |
| 不兼容的接口、认证、配置或升级路径变更 | MAJOR | `1.1.0 → 2.0.0` |
| 候选版迭代 | RC 序号递增 | `1.0.0-rc.1 → 1.0.0-rc.2` |
| 候选版转正式版 | 去掉 RC 后缀并重新验收 | `1.0.0-rc.2 → 1.0.0` |

当前基线为 `1.0.0-rc.1`。源码版本不代表已发布；正式发布记录以 `v<版本>` Tag 和 GitHub Release 为准。已发布版本不可覆盖，修复必须使用更高版本。

## 日常维护

- 从 `main` 创建短期工作分支，通过 PR 合入；日常开发不逐次修改版本号。
- 将影响用户的变更写入根 `CHANGELOG.md` 的 `Unreleased`，注明不兼容变化。
- 根据实际兼容性决定升级级别，不仅凭提交类型判断。
- 浏览器存储结构版本 `VITE_APP_STORAGE_NAMESPACE` 独立维护，普通产品升版不修改它。

## 准备新版本

以准备 `1.0.0` 为例，在发布准备分支执行：

1. 获取最新 Tag：`git fetch origin main --tags`。
2. 整理 `CHANGELOG.md` 的 `Unreleased`，内容不能为空。
3. 编写 `docs/release/1.0.0.md`，包含「升级来源」「数据库」「停机与会话」「回滚」四部分，可参考 [候选版说明](1.0.0-rc.1.md)。
4. 执行以下命令并审查差异：

   ```bash
   pnpm release:prepare --version 1.0.0
   pnpm version:check
   pnpm install --frozen-lockfile
   ```

5. 使用 `pnpm run commit` 提交发布准备 PR，例如 `release: prepare 1.0.0`。

`release:prepare` 要求新版本高于当前版本及已有 Tag，自动同步各模块版本，并将 `Unreleased` 归档到新版本。它不会提交、推送或创建 Tag；不要预先手改根版本，也不要再次 prepare 相同版本。

## 校验与常见处理

| 目的或问题 | 操作 |
| --- | --- |
| 检查版本一致性 | `pnpm version:check` |
| 修复模块版本漂移 | 确认根版本正确后执行 `pnpm version:sync`，再审查差异 |
| 未提交代码的完整演练 | `pnpm release:verify --local`，通过后执行 `pnpm release:bundle --local`；演练包不可正式发布 |
| 干净工作区的发布验收 | `pnpm release:verify`，通过后执行 `pnpm release:bundle` |
| 验收后源码或产物变化 | 重新验收，不复用旧凭据或覆盖已有输出目录 |
| 已发布 SQL 需要修改 | 追加迁移，不能修改历史脚本或校验和，见 [数据库规则](../../apps/server/deploy/migrations/README.md) |

完整验收需要 Docker、Chromium、Java 17、Maven、Node.js、pnpm 和 zip/unzip。发布准备 PR 合入后，在已配置好的 GitHub `Release` 工作流中选择 `main`，使用合入后的完整提交 SHA 发布。
