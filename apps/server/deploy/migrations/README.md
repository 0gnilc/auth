# 数据库版本与执行记录

`manifest.json` 声明数据库基线、文件顺序、SHA-256、已支持的升级来源和后续有序迁移。产品版本与迁移 ID 独立。当前 `baseline-1` 仅用于全新安装，`supportedUpgradeFrom: []` 表示不支持从已有版本自动升级。

- `baseline.files`：按数组顺序执行的基线文件，当前是原有 01–07 初始化脚本及 `history.sql`。01–07 必须处于同一个 MySQL 连接，保留默认管理员授权所需会话变量。
- `migrations`：未来增量 SQL，每项包含 `id`（UTC 时间戳及名称，如 `20261001090000_add_admin_field`）、`path`、`sha256` 和中文 `description`。同一 ID 只允许执行一次。
- 正式/候选 Tag 出现后，已登记的 SQL 内容与迁移标识不可修改或删除；修正应追加新迁移。校验命令会对比本地已抓取的发布 Tag。
- 初始化和迁移由部署操作员在维护窗口执行，不由应用启动或 Release 工作流自动连接数据库执行。

执行前核验 `SHA256SUMS` 和发布清单，备份并验证恢复能力。取得部署独占锁，读取 `sys_schema_history`，核对已有记录的校验和与支持的升级来源。未知来源、缺失前置迁移或校验和不同必须中止。

新库完成所有基线 SQL 后，在同一维护窗口记录 `migration_id = baseline-1`。`checksum_sha256` 是按 manifest 顺序拼接各文件 SHA-256（每行一个，末行也有换行）后计算的 SHA-256。Node 示例：

```js
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
const manifest = JSON.parse(readFileSync('apps/server/deploy/migrations/manifest.json', 'utf8'));
console.log(createHash('sha256').update(manifest.baseline.files.map((entry) => `${entry.sha256}\n`).join('')).digest('hex'));
```

每次成功迁移后向 `sys_schema_history` 插入一条记录：迁移 ID、发布清单校验和、产品版本、完整 Git SHA、执行人。数据库连接时区设为 UTC。只允许 INSERT，不使用 REPLACE 或覆盖历史记录；部署审计另行保存开始时间、结束时间、失败详情及备份位置。

MySQL DDL 可能隐式提交。失败时停止后续迁移，保留日志并按已审核恢复方案处理；不能写入成功记录，也不能自动重跑可能已执行一部分的 SQL。本目录提供清单验证与记录表，不提供通用 SQL 自动执行器。未来首次实际升级必须补充对应迁移及隔离 MySQL 的升级/回滚测试。
