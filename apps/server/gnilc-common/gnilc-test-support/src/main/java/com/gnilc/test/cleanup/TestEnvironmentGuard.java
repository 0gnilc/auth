package com.gnilc.test.cleanup;

import com.gnilc.test.container.SharedTestContainers;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 在执行截表或 {@code FLUSHDB} 前验证 profile、显式开关和实际容器端点。
 */
public final class TestEnvironmentGuard {
    private final Environment environment;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String expectedJdbcUrl;
    private final String expectedRedisHost;
    private final int expectedRedisPort;
    private final int expectedRedisDatabase;

    /**
     * @param environment 当前 Spring 环境
     * @param dataSource 待清理的实际数据源
     * @param redisConnectionFactory 待清理的实际 Redis 连接工厂
     * @param expectedJdbcUrl 当前共享 MySQL 的 JDBC URL
     * @param expectedRedisHost 当前共享 Redis 的主机名
     * @param expectedRedisPort 当前共享 Redis 的映射端口
     * @param expectedRedisDatabase 允许清理的 Redis 数据库索引
     */
    public TestEnvironmentGuard(Environment environment,
                                DataSource dataSource,
                                RedisConnectionFactory redisConnectionFactory,
                                String expectedJdbcUrl,
                                String expectedRedisHost,
                                int expectedRedisPort,
                                int expectedRedisDatabase) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.expectedJdbcUrl = expectedJdbcUrl;
        this.expectedRedisHost = expectedRedisHost;
        this.expectedRedisPort = expectedRedisPort;
        this.expectedRedisDatabase = expectedRedisDatabase;
    }

    /**
     * 验证当前环境只会清理本测试 JVM 启动的 MySQL 和 Redis。
     *
     * @throws IllegalStateException 缺少测试开关、端点不匹配或无法检查连接时
     */
    public void assertCleanupAllowed() {
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("test"::equals)) {
            throw new IllegalStateException("Refusing cleanup outside the test profile");
        }
        if (!environment.getProperty("app.test.cleanup.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Refusing cleanup without app.test.cleanup.enabled");
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!SharedTestContainers.DATABASE_NAME.equals(connection.getCatalog())) {
                throw new IllegalStateException("Refusing cleanup for database " + connection.getCatalog());
            }
            String actualJdbcUrl = connection.getMetaData().getURL();
            if (!jdbcEndpoint(expectedJdbcUrl).equals(jdbcEndpoint(actualJdbcUrl))) {
                throw new IllegalStateException("Refusing cleanup for non-container MySQL endpoint");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect the cleanup database", exception);
        }
        if (!(redisConnectionFactory instanceof LettuceConnectionFactory lettuce)) {
            throw new IllegalStateException("Refusing cleanup for an unrecognized Redis connection factory");
        }
        RedisStandaloneConfiguration redis = lettuce.getStandaloneConfiguration();
        if (!expectedRedisHost.equals(redis.getHostName())
                || expectedRedisPort != redis.getPort()
                || expectedRedisDatabase != redis.getDatabase()) {
            throw new IllegalStateException("Refusing cleanup for non-container Redis endpoint");
        }
    }

    private String jdbcEndpoint(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        return queryStart < 0 ? jdbcUrl : jdbcUrl.substring(0, queryStart);
    }
}
