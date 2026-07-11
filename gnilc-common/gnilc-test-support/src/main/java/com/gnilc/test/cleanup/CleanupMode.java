package com.gnilc.test.cleanup;

/**
 * 测试数据隔离策略。
 */
public enum CleanupMode {
    /** 不执行显式清理，适用于无持久化状态的测试。 */
    NONE,
    /** 依赖同线程测试事务回滚，不执行额外清理。 */
    TRANSACTION_ROLLBACK,
    /** 在测试前后清空测试容器拥有的 Redis 数据库。 */
    REDIS_CLEAN,
    /** 清空 MySQL 与 Redis，并在测试前恢复业务基线数据。 */
    BASELINE_RESET
}
