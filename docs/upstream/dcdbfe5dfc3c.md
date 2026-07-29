---
upstream_repository: https://github.com/vbenjs/vue-vben-admin
upstream_commit: dcdbfe5dfc3c2b242f43950dff9ac4e97120a4ca
upstream_date: 2026-07-24
status: partial
reviewed_at: 2026-07-30
local_commits:
  - 3508304cbc556211cc9177ed437fe8ea314b6c69
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

状态标记为 `partial`。锁屏提交、表单 API、当前 Admin 业务表单与 vxe-table 修复已随连续表单迁移完成；文档站、playground 日期范围和演示代码永久过滤。当前没有待后续同步内容。

### 已同步内容

- 锁屏表单使用 `getRawValues()` 读取密码，并通过 `setFieldError()` 回写错误。
- 表单 API、字段错误、提交值转换和运行时修复。
- vxe-table API、泛型、查询与重置行为，以及当前 Admin 表格适配器。
- 当前项目仍使用的业务表单与个人设置表单适配。

### 待后续同步内容

无。

### 永久过滤内容

- 上游文档站、playground 日期范围工具和演示代码。

## 本地调整

- 保留锁屏的本地开关默认值和认证流程，仅同步表单取值与错误处理。
- 管理员、菜单和动态国际化表单继续保留权限动作、未保存变更保护与 Message Key 持久化边界。
- 上游已移除的 Shadcn `Form` 不再用于个人资料设置包装，改用语义等价的原生 `<form>`。
- vxe-table 保留本地组件注册与请求契约，同时接入新的 typed form API。

## 验证

- 表单专项测试通过，共 8 个文件、82 个测试；Admin 测试通过，共 60 个文件、457 个测试。
- Admin 类型检查、`pnpm run check`、`pnpm run lint` 和 `pnpm run build:admin` 通过。
- 两组表单性能基准和 `git diff --check` 通过。
