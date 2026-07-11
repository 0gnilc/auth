package com.gnilc.test.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Driver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JVM-scoped MySQL and Redis containers used by integration tests.
 */
public final class SharedTestContainers {
    public static final String DATABASE_NAME = "gnilc_auth_test";

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse(System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName(DATABASE_NAME)
            .withUsername("test")
            .withPassword("test");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse(System.getProperty("app.test.redis.image", "redis:8-alpine")))
            .withExposedPorts(6379);

    private static final Set<String> INITIALIZED_SCHEMAS = new HashSet<>();

    private SharedTestContainers() {
    }

    public static synchronized MySQLContainer<?> mysql() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        return MYSQL;
    }

    public static synchronized GenericContainer<?> redis() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        return REDIS;
    }

    public static synchronized void initializeMySqlSchema(String... classpathLocations) {
        mysql();
        String schemaKey = String.join("\n", classpathLocations);
        if (!INITIALIZED_SCHEMAS.add(schemaKey)) {
            return;
        }
        try {
            Driver driver = (Driver) Class.forName(MYSQL.getDriverClassName()).getDeclaredConstructor().newInstance();
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource(
                    driver, MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(Arrays.stream(classpathLocations)
                    .map(ClassPathResource::new)
                    .toArray(ClassPathResource[]::new));
            populator.execute(dataSource);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            INITIALIZED_SCHEMAS.remove(schemaKey);
            throw new IllegalStateException("Failed to initialize the test schema", exception);
        }
    }
}
