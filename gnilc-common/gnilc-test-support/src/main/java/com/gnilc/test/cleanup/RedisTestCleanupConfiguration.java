package com.gnilc.test.cleanup;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

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
