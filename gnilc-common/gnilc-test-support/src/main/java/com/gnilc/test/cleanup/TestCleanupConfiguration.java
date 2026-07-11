package com.gnilc.test.cleanup;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.util.List;

/**
 * 同时使用 MySQL 与 Redis 的集成测试清理配置。
 * <p>
 * 业务模块可以额外提供多个 {@link BaselineDataSeeder}，由重置管理器在清理后统一执行。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestCleanupConfiguration {
    @Bean
    TestEnvironmentGuard testEnvironmentGuard() {
        return new TestEnvironmentGuard();
    }

    @Bean
    DatabaseCleaner databaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource);
    }

    @Bean
    RedisCleaner redisCleaner(RedisConnectionFactory connectionFactory) {
        return new RedisCleaner(connectionFactory);
    }

    @Bean
    TestDataResetManager testDataResetManager(Environment environment,
                                              DataSource dataSource,
                                              DatabaseCleaner databaseCleaner,
                                              RedisCleaner redisCleaner,
                                              TestEnvironmentGuard guard,
                                              List<BaselineDataSeeder> seeders) {
        return new TestDataResetManager(environment, dataSource,
                databaseCleaner, redisCleaner, guard, seeders);
    }
}
