package com.gnilc.test.cleanup;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDataResetManagerTest {
    @Test
    void baselineResetGuardsThenCleansSeedsAndCleansCacheAgain() {
        TestEnvironmentGuard guard = mock(TestEnvironmentGuard.class);
        DatabaseCleaner database = mock(DatabaseCleaner.class);
        RedisCleaner redis = mock(RedisCleaner.class);
        BaselineDataSeeder first = mock(BaselineDataSeeder.class);
        BaselineDataSeeder second = mock(BaselineDataSeeder.class);
        TestDataResetManager manager =
                new TestDataResetManager(guard, database, redis, List.of(first, second));

        manager.resetToBaseline();

        InOrder order = inOrder(guard, redis, database, first, second);
        order.verify(guard).assertCleanupAllowed();
        order.verify(redis).flushDatabase();
        order.verify(database).truncateBusinessTables();
        order.verify(first).seed();
        order.verify(second).seed();
        order.verify(redis).flushDatabase();
    }

    @Test
    void afterTestAlwaysCleansBothStores() {
        TestEnvironmentGuard guard = mock(TestEnvironmentGuard.class);
        DatabaseCleaner database = mock(DatabaseCleaner.class);
        RedisCleaner redis = mock(RedisCleaner.class);

        new TestDataResetManager(guard, database, redis, List.of()).cleanAfterTest();

        verify(guard).assertCleanupAllowed();
        verify(redis).flushDatabase();
        verify(database).truncateBusinessTables();
    }

    @Test
    void afterTestAttemptsDatabaseCleanupWhenRedisCleanupFails() {
        TestEnvironmentGuard guard = mock(TestEnvironmentGuard.class);
        DatabaseCleaner database = mock(DatabaseCleaner.class);
        RedisCleaner redis = mock(RedisCleaner.class);
        IllegalStateException failure = new IllegalStateException("redis unavailable");
        doThrow(failure).when(redis).flushDatabase();

        assertThatThrownBy(() -> new TestDataResetManager(guard, database, redis, List.of()).cleanAfterTest())
                .isSameAs(failure);

        verify(database).truncateBusinessTables();
    }
}
