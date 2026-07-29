---
upstream_repository: https://github.com/vbenjs/vue-vben-admin
upstream_commit: dcdbfe5dfc3c2b242f43950dff9ac4e97120a4ca
upstream_date: 2026-07-24
status: pending
reviewed_at: 2026-07-29
local_commits: []
scope:
  - apps/admin
  - packages/@core/ui-kit/form-ui
  - packages/effects/layouts
  - packages/effects/plugins
---

# 上游提交：恢复锁屏表单提交并修复表单 API

上游提交：<https://github.com/vbenjs/vue-vben-admin/commit/dcdbfe5dfc3c2b242f43950dff9ac4e97120a4ca>

## 影响评估

该提交修复迁移后的表单 API、锁屏表单提交和 vxe-table 表单处理，同时更新上游文档、日期范围工具及示例。当前项目保留锁屏组件、表单包和 vxe-table 插件，但本地业务表单需要一起适配。

## 同步决定

状态标记为 `pending`。与 Zod 4、TanStack Form 连续迁移一并实施；文档站、playground 日期范围和演示代码永久过滤。

## 本地调整

尚未实施。

## 验证

已完成影响范围评估，锁屏和本地业务表单验证待同步后执行。
