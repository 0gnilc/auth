---
upstream_repository: https://github.com/vbenjs/vue-vben-admin
upstream_commit: fbf0cebf4bf010dac4dd34eaf1f218bc85d39edb
upstream_date: 2026-07-27
status: synced
reviewed_at: 2026-07-30
local_commits:
  - 2d13c63fd669ccac054f37dae231f9a605357a5d
scope:
  - packages/effects/plugins
---

# 上游提交：重组 vxe-table viewed-row 类型

上游提交：<https://github.com/vbenjs/vue-vben-admin/commit/fbf0cebf4bf010dac4dd34eaf1f218bc85d39edb>

## 影响评估

该提交将 viewed-row 类型和 composable 从 vxe-table 公共类型文件移动到独立功能目录，并更新 API 与 grid 引用。当前项目继续使用 vxe-table，并有本地管理表格适配器。

## 同步决定

状态标记为 `synced`。上游提交的目录、类型和导入调整已全部进入当前项目，没有过滤内容；Admin 管理表格适配器没有引用旧的内部路径或被移动的类型。

## 本地调整

- 将 `use-viewed-row.ts` 原样移动到 `viewed-row/`，并新增该功能域的统一出口。
- 将 viewed-row 配置和存储类型从 vxe-table 公共 `types.ts` 移入 `viewed-row/types.ts`。
- 更新 `api.ts`、`types.ts` 和 `use-vxe-grid.vue` 的内部导入路径。
- 保留当前项目迁移前的表单 API、表单泛型和 `resetForm` 行为；这些差异来自尚未同步的连续表单迁移，不属于本提交的变更。
- 使用零上下文差异计算的本地补丁指纹与上游提交一致，确认本提交增量已全量映射。

## 验证

- Admin 表格契约测试通过，共 1 个测试。
- `pnpm run test:admin` 通过，共 53 个测试文件、389 个测试。
- `pnpm --filter=@app/admin run typecheck` 通过。
- `pnpm run lint` 通过。
- `pnpm run build:admin` 通过；仅保留第三方 `@vueuse/core` PURE annotation 警告。
- `git diff --check` 通过。
