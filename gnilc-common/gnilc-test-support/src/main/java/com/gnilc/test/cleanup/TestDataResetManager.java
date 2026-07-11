package com.gnilc.test.cleanup;

import java.util.List;

/**
 * 编排清理保护、MySQL/Redis 清理和业务模块基线恢复。
 */
public final class TestDataResetManager {
    private final TestEnvironmentGuard guard;
    private final DatabaseCleaner databaseCleaner;
    private final RedisCleaner redisCleaner;
    private final List<BaselineDataSeeder> seeders;

    /**
     * @param guard 破坏性清理前的环境保护器
     * @param databaseCleaner MySQL 业务表清理器
     * @param redisCleaner Redis 数据库清理器
     * @param seeders 由业务模块提供并已排序的基线写入器
     */
    public TestDataResetManager(TestEnvironmentGuard guard,
                                DatabaseCleaner databaseCleaner,
                                RedisCleaner redisCleaner,
                                List<BaselineDataSeeder> seeders) {
        this.guard = guard;
        this.databaseCleaner = databaseCleaner;
        this.redisCleaner = redisCleaner;
        this.seeders = List.copyOf(seeders);
    }

    /** 清理两类存储、写入业务基线，再清除基线写入过程中产生的缓存。 */
    public void resetToBaseline() {
        guard.assertCleanupAllowed();
        cleanStores();
        seeders.forEach(BaselineDataSeeder::seed);
        redisCleaner.flushDatabase();
    }

    /** 测试方法结束后尽力清理 Redis 和 MySQL，不因一方失败而跳过另一方。 */
    public void cleanAfterTest() {
        guard.assertCleanupAllowed();
        cleanStores();
    }

    private void cleanStores() {
        RuntimeException failure = null;
        try {
            redisCleaner.flushDatabase();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            databaseCleaner.truncateBusinessTables();
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
