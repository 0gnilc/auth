package com.gnilc.test.cleanup;

import com.gnilc.test.container.SharedTestContainers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class TestCleanupConfiguration {
    @Bean
    TestEnvironmentGuard testEnvironmentGuard(Environment environment,
                                              DataSource dataSource,
                                              RedisConnectionFactory redisConnectionFactory) {
        MySQLContainer<?> mysql = SharedTestContainers.mysql();
        GenericContainer<?> redis = SharedTestContainers.redis();
        return new TestEnvironmentGuard(
                environment,
                dataSource,
                redisConnectionFactory,
                mysql.getJdbcUrl(),
                redis.getHost(),
                redis.getMappedPort(6379),
                0);
    }

    @Bean
    DatabaseCleaner databaseCleaner(JdbcTemplate jdbcTemplate) {
        return new DatabaseCleaner(jdbcTemplate);
    }

    @Bean
    RedisCleaner redisCleaner(RedisConnectionFactory connectionFactory) {
        return new RedisCleaner(connectionFactory);
    }

    @Bean
    TestDataResetManager testDataResetManager(TestEnvironmentGuard guard,
                                              DatabaseCleaner databaseCleaner,
                                              RedisCleaner redisCleaner,
                                              ObjectProvider<BaselineDataSeeder> seeders) {
        return new TestDataResetManager(guard, databaseCleaner, redisCleaner, seeders.orderedStream().toList());
    }
}
