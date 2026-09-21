-- 由部署操作员在经过审核的初始化/迁移成功后记录；应用启动不会自动执行。
CREATE TABLE IF NOT EXISTS sys_schema_history (
    migration_id VARCHAR(100) NOT NULL COMMENT '不可变的迁移标识，基线使用 baseline-1',
    checksum_sha256 CHAR(64) NOT NULL COMMENT '发布清单中的迁移内容 SHA-256',
    product_version VARCHAR(40) NOT NULL COMMENT '执行该迁移的产品版本',
    git_revision CHAR(40) NOT NULL COMMENT '发布源码的完整 Git 提交 SHA',
    applied_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '迁移执行完成时间，连接必须使用 UTC',
    applied_by VARCHAR(100) NOT NULL COMMENT '执行操作员或部署任务标识',
    PRIMARY KEY (migration_id)
) COMMENT='数据库迁移执行记录，成功记录不可覆盖';
