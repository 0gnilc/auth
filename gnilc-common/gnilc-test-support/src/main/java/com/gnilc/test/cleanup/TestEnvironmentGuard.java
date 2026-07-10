package com.gnilc.test.cleanup;

import com.gnilc.test.container.MySqlContainerSupport;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class TestEnvironmentGuard {
    public void verifyDatabase(Environment environment, DataSource dataSource) {
        verifyCommon(environment);
        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();
            URI jdbcEndpoint = jdbcEndpoint(connection.getMetaData().getURL());
            String ownedHost = environment.getProperty("app.test.container.mysql.host");
            Integer ownedPort = environment.getProperty("app.test.container.mysql.port", Integer.class);
            if (!MySqlContainerSupport.DATABASE_NAME.equals(catalog)
                    || !java.util.Objects.equals(jdbcEndpoint.getHost(), ownedHost)
                    || !Integer.valueOf(jdbcEndpoint.getPort()).equals(ownedPort)) {
                throw new IllegalStateException("Refusing cleanup for a database not owned by the test container");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to verify test database", e);
        }
    }

    public void verifyRedis(Environment environment) {
        verifyCommon(environment);
        String host = environment.getProperty("spring.data.redis.host");
        Integer port = environment.getProperty("spring.data.redis.port", Integer.class);
        String ownedHost = environment.getProperty("app.test.container.redis.host");
        Integer ownedPort = environment.getProperty("app.test.container.redis.port", Integer.class);
        if (!java.util.Objects.equals(ownedHost, host) || !java.util.Objects.equals(ownedPort, port)) {
            throw new IllegalStateException("Refusing cleanup for Redis not owned by the test container");
        }
    }

    private void verifyCommon(Environment environment) {
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        boolean cleanupEnabled = environment.getProperty("app.test.cleanup.enabled", Boolean.class, false);
        boolean containerOwned = environment.getProperty("app.test.container.owned", Boolean.class, false);
        if (!testProfile || !cleanupEnabled || !containerOwned) {
            throw new IllegalStateException("Destructive cleanup requires an owned test-container environment");
        }
    }

    private URI jdbcEndpoint(String jdbcUrl) {
        try {
            return URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid JDBC URL", e);
        }
    }
}
