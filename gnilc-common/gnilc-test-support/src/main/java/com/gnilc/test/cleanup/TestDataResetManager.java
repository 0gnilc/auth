package com.gnilc.test.cleanup;

import com.gnilc.test.annotation.CleanTestData;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 协调测试环境校验、Redis/MySQL 清理和业务基线数据恢复。
 * <p>
 * 所有破坏性操作都先通过 {@link TestEnvironmentGuard}；仅 Redis 的配置允许数据库相关依赖为空，
 * 此时只能使用 {@link CleanupMode#REDIS_CLEAN}。
 */
public class TestDataResetManager {
    private final Environment environment;
    private final DataSource dataSource;
    private final DatabaseCleaner databaseCleaner;
    private final RedisCleaner redisCleaner;
    private final TestEnvironmentGuard guard;
    private final List<BaselineDataSeeder> seeders;

    /**
     * 创建测试数据重置管理器。
     *
     * @param environment Spring 测试环境，用于校验 Profile 与容器归属标记
     * @param dataSource 测试数据库数据源；仅 Redis 的清理配置中可以为空
     * @param databaseCleaner 数据库清理器；仅 Redis 的清理配置中可以为空
     * @param redisCleaner Redis 清理器
     * @param guard 破坏性操作环境守卫
     * @param seeders 数据库清理后按顺序执行的基线数据播种器，可以为空
     */
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

    /**
     * 在测试方法执行前按指定模式重置数据。
     * <p>
     * {@link CleanupMode#BASELINE_RESET} 会依次校验环境、清理 Redis、清理数据库并恢复基线数据。
     *
     * @param mode 当前测试声明的清理模式；为空时按 {@link CleanupMode#NONE} 处理
     */
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

    /**
     * 在测试方法执行后按指定模式执行收尾清理，不再恢复基线数据。
     * <p>
     * 全栈清理会分别尝试 Redis 与数据库，即使 Redis 清理失败也会继续清理数据库，
     * 并通过 suppressed exception 保留第二个失败。
     *
     * @param mode 当前测试声明的清理模式；为空时按 {@link CleanupMode#NONE} 处理
     */
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

    /**
     * 解析测试方法最终生效的清理模式，方法级注解优先于类级注解。
     *
     * @param testClass 当前测试类
     * @param testMethod 当前测试方法，可以为空
     * @return 最终清理模式；未声明时返回 {@link CleanupMode#NONE}
     */
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
