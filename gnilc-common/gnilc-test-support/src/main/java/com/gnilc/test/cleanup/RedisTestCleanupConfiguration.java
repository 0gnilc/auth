package com.gnilc.test.cleanup;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 仅依赖 Redis 的集成测试清理配置。
 * <p>
 * 该配置不创建数据库清理器，只支持不触碰 MySQL 的 {@link CleanupMode#REDIS_CLEAN}。
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestCleanupConfiguration {
    @Bean
    TestEnvironmentGuard testEnvironmentGuard() {
        return new TestEnvironmentGuard();
    }

    @Bean
    RedisCleaner redisCleaner(RedisConnectionFactory connectionFactory) {
        return new RedisCleaner(connectionFactory);
    }

    @Bean
    TestDataResetManager testDataResetManager(Environment environment,
                                              RedisCleaner redisCleaner,
                                              TestEnvironmentGuard guard) {
        return new TestDataResetManager(environment, null, null, redisCleaner, guard, null);
    }
}
