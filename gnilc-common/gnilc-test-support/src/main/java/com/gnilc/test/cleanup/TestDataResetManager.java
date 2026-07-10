package com.gnilc.test.cleanup;

import com.gnilc.test.annotation.CleanTestData;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;

public class TestDataResetManager {
    private final Environment environment;
    private final DataSource dataSource;
    private final DatabaseCleaner databaseCleaner;
    private final RedisCleaner redisCleaner;
    private final TestEnvironmentGuard guard;
    private final List<BaselineDataSeeder> seeders;

    public TestDataResetManager(Environment environment,
                                DataSource dataSource,
                                DatabaseCleaner databaseCleaner,
                                RedisCleaner redisCleaner,
                                TestEnvironmentGuard guard,
                                List<BaselineDataSeeder> seeders) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.databaseCleaner = databaseCleaner;
        this.redisCleaner = redisCleaner;
        this.guard = guard;
        this.seeders = seeders == null ? List.of() : List.copyOf(seeders);
    }

    public void reset(CleanupMode mode) {
        if (mode == null || mode == CleanupMode.NONE || mode == CleanupMode.TRANSACTION_ROLLBACK) {
            return;
        }
        guard.verifyRedis(environment);
        if (mode == CleanupMode.BASELINE_RESET) {
            guard.verifyDatabase(environment, dataSource);
        }
        redisCleaner.clean();
        if (mode == CleanupMode.BASELINE_RESET) {
            databaseCleaner.clean();
            seeders.forEach(BaselineDataSeeder::seed);
        }
    }

    public void cleanupAfter(CleanupMode mode) {
        if (mode == null || mode == CleanupMode.NONE || mode == CleanupMode.TRANSACTION_ROLLBACK) {
            return;
        }
        guard.verifyRedis(environment);
        if (mode == CleanupMode.BASELINE_RESET) {
            guard.verifyDatabase(environment, dataSource);
        }
        if (mode == CleanupMode.REDIS_CLEAN) {
            redisCleaner.clean();
            return;
        }
        RuntimeException failure = null;
        try {
            redisCleaner.clean();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            databaseCleaner.clean();
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

    public static CleanupMode mode(Class<?> testClass, Method testMethod) {
        CleanTestData methodAnnotation = testMethod == null ? null
                : AnnotatedElementUtils.findMergedAnnotation(testMethod, CleanTestData.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }
        CleanTestData classAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, CleanTestData.class);
        return classAnnotation == null ? CleanupMode.NONE : classAnnotation.value();
    }
}
