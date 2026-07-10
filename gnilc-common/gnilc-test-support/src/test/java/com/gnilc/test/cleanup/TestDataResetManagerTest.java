package com.gnilc.test.cleanup;

import com.gnilc.test.annotation.CleanTestData;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TestDataResetManagerTest {
    private final Environment environment = mock(Environment.class);
    private final DataSource dataSource = mock(DataSource.class);
    private final DatabaseCleaner databaseCleaner = mock(DatabaseCleaner.class);
    private final RedisCleaner redisCleaner = mock(RedisCleaner.class);
    private final TestEnvironmentGuard guard = mock(TestEnvironmentGuard.class);

    @Test
    void baselineResetVerifiesBeforeCleaningAndSeedsAfterBothStoresAreClean() {
        List<String> events = new ArrayList<>();
        BaselineDataSeeder first = () -> events.add("seed-one");
        BaselineDataSeeder second = () -> events.add("seed-two");
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, List.of(first, second));

        manager.reset(CleanupMode.BASELINE_RESET);

        var ordered = inOrder(guard, redisCleaner, databaseCleaner);
        ordered.verify(guard).verifyRedis(environment);
        ordered.verify(guard).verifyDatabase(environment, dataSource);
        ordered.verify(redisCleaner).clean();
        ordered.verify(databaseCleaner).clean();
        assertThat(events).containsExactly("seed-one", "seed-two");
    }

    @Test
    void baselineCleanupDoesNotReseed() {
        BaselineDataSeeder seeder = mock(BaselineDataSeeder.class);
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, List.of(seeder));

        manager.cleanupAfter(CleanupMode.BASELINE_RESET);

        var ordered = inOrder(guard, redisCleaner, databaseCleaner);
        ordered.verify(guard).verifyRedis(environment);
        ordered.verify(guard).verifyDatabase(environment, dataSource);
        ordered.verify(redisCleaner).clean();
        ordered.verify(databaseCleaner).clean();
        verifyNoInteractions(seeder);
    }

    @Test
    void baselineCleanupAttemptsDatabaseWhenRedisCleanupFails() {
        RuntimeException redisFailure = new IllegalStateException("redis failed");
        doThrow(redisFailure).when(redisCleaner).clean();
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> manager.cleanupAfter(CleanupMode.BASELINE_RESET))
                .isSameAs(redisFailure);

        verify(databaseCleaner).clean();
    }

    @Test
    void baselineCleanupPreservesBothFailures() {
        RuntimeException redisFailure = new IllegalStateException("redis failed");
        RuntimeException databaseFailure = new IllegalStateException("database failed");
        doThrow(redisFailure).when(redisCleaner).clean();
        doThrow(databaseFailure).when(databaseCleaner).clean();
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> manager.cleanupAfter(CleanupMode.BASELINE_RESET))
                .isSameAs(redisFailure)
                .satisfies(exception -> assertThat(exception.getSuppressed())
                        .containsExactly(databaseFailure));
    }

    @Test
    void databaseGuardRefusalPreventsAllDestructiveActions() {
        BaselineDataSeeder seeder = mock(BaselineDataSeeder.class);
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, List.of(seeder));
        doThrow(new IllegalStateException("refused"))
                .when(guard).verifyDatabase(environment, dataSource);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> manager.reset(CleanupMode.BASELINE_RESET))
                .hasMessage("refused");

        verifyNoInteractions(databaseCleaner, redisCleaner, seeder);
    }

    @Test
    void redisCleanTouchesOnlyRedis() {
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, null);

        manager.reset(CleanupMode.REDIS_CLEAN);

        var ordered = inOrder(guard, redisCleaner);
        ordered.verify(guard).verifyRedis(environment);
        ordered.verify(redisCleaner).clean();
        verifyNoInteractions(databaseCleaner, dataSource);
    }

    @Test
    void nonDestructiveModesDoNothing() {
        TestDataResetManager manager = new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, null);

        manager.reset(CleanupMode.NONE);
        manager.cleanupAfter(CleanupMode.TRANSACTION_ROLLBACK);
        manager.reset(null);

        verifyNoInteractions(environment, dataSource, databaseCleaner, redisCleaner, guard);
    }

    @Test
    void methodAnnotationOverridesClassAnnotationAndMissingAnnotationMeansNone() throws Exception {
        assertThat(TestDataResetManager.mode(AnnotatedTests.class,
                AnnotatedTests.class.getDeclaredMethod("redisOnly")))
                .isEqualTo(CleanupMode.REDIS_CLEAN);
        assertThat(TestDataResetManager.mode(AnnotatedTests.class,
                AnnotatedTests.class.getDeclaredMethod("baseline")))
                .isEqualTo(CleanupMode.BASELINE_RESET);
        assertThat(TestDataResetManager.mode(UnannotatedTests.class,
                UnannotatedTests.class.getDeclaredMethod("test")))
                .isEqualTo(CleanupMode.NONE);
    }

    @CleanTestData(CleanupMode.BASELINE_RESET)
    private static class AnnotatedTests {
        @CleanTestData(CleanupMode.REDIS_CLEAN)
        void redisOnly() {
        }

        void baseline() {
        }
    }

    private static class UnannotatedTests {
        void test() {
        }
    }
}
