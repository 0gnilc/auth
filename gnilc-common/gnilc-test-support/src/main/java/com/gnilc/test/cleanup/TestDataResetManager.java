package com.gnilc.test.cleanup;

import java.util.List;

public final class TestDataResetManager {
    private final TestEnvironmentGuard guard;
    private final DatabaseCleaner databaseCleaner;
    private final RedisCleaner redisCleaner;
    private final List<BaselineDataSeeder> seeders;

    public TestDataResetManager(TestEnvironmentGuard guard,
                                DatabaseCleaner databaseCleaner,
                                RedisCleaner redisCleaner,
                                List<BaselineDataSeeder> seeders) {
        this.guard = guard;
        this.databaseCleaner = databaseCleaner;
        this.redisCleaner = redisCleaner;
        this.seeders = List.copyOf(seeders);
    }

    public void resetToBaseline() {
        guard.assertCleanupAllowed();
        cleanStores();
        seeders.forEach(BaselineDataSeeder::seed);
        redisCleaner.flushDatabase();
    }

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
