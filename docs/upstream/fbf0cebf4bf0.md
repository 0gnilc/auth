---
upstream_repository: https://github.com/vbenjs/vue-vben-admin
upstream_commit: fbf0cebf4bf010dac4dd34eaf1f218bc85d39edb
upstream_date: 2026-07-27
status: pending
reviewed_at: 2026-07-29
local_commits: []
scope:
  - packages/effects/plugins
---

# 上游提交：重组 vxe-table viewed-row 类型

上游提交：<https://github.com/vbenjs/vue-vben-admin/commit/fbf0cebf4bf010dac4dd34eaf1f218bc85d39edb>

## 影响评估

该提交将 viewed-row 类型和 composable 从 vxe-table 公共类型文件移动到独立功能目录，并更新 API 与 grid 引用。当前项目继续使用 vxe-table，并有本地管理表格适配器。

## 同步决定

状态标记为 `pending`。上游重组可以完整同步，但需要确认本地导入路径和管理表格扩展没有依赖旧的公共导出。

## 本地调整

尚未实施。

## 验证

已完成文件范围评估，类型检查、表格测试和 Admin 构建待同步后执行。
