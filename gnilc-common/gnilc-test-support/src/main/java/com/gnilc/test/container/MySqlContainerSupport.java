package com.gnilc.test.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class MySqlContainerSupport {
    public static final String DATABASE_NAME = "gnilc_auth_test";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static final String MARKER_PROPERTY = "app.test.container.owned";
    private static final MySQLContainer<?> CONTAINER = new MySQLContainer<>(mysqlImage())
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withReuse(false);
    private static final Set<String> INITIALIZED_SCRIPTS = new LinkedHashSet<>();

    private MySqlContainerSupport() {
    }

    public static synchronized MySQLContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    public static void applyProperties(BiConsumer<String, Object> properties) {
        MySQLContainer<?> mysql = container();
        applyProperties(properties, mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword(),
                mysql.getDriverClassName(), mysql.getHost(), mysql.getMappedPort(MySQLContainer.MYSQL_PORT));
    }

    static void applyProperties(BiConsumer<String, Object> properties,
                                String jdbcUrl,
                                String username,
                                String password,
                                String driverClassName,
                                String host,
                                int port) {
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        properties.accept("spring.datasource.url", jdbcUrl + separator + "useAffectedRows=true");
        properties.accept("spring.datasource.username", username);
        properties.accept("spring.datasource.password", password);
        properties.accept("spring.datasource.driver-class-name", driverClassName);
        properties.accept("app.test.cleanup.enabled", true);
        properties.accept(MARKER_PROPERTY, true);
        properties.accept("app.test.container.mysql.host", host);
        properties.accept("app.test.container.mysql.port", port);
    }

    public static synchronized void initializeSchema(String... classpathScripts) {
        container();
        try (Connection connection = DriverManager.getConnection(
                CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword())) {
            for (String script : Arrays.asList(classpathScripts)) {
                if (INITIALIZED_SCRIPTS.contains(script)) {
                    continue;
                }
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(script));
                INITIALIZED_SCRIPTS.add(script);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize MySQL test schema", e);
        }
    }

    private static DockerImageName mysqlImage() {
        return DockerImageName.parse(System.getProperty("app.test.mysql.image", "mysql:8.4.0"));
    }
}
