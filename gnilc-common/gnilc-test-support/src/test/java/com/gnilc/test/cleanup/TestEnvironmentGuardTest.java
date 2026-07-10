package com.gnilc.test.cleanup;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestEnvironmentGuardTest {
    private final TestEnvironmentGuard guard = new TestEnvironmentGuard();

    @Test
    void refusesCleanupWithoutTestProfile() {
        MockEnvironment environment = ownedEnvironment();
        environment.setActiveProfiles("other");
        assertThatThrownBy(() -> guard.verifyRedis(environment)).hasMessageContaining("owned test-container");
    }

    @Test
    void refusesCleanupWithoutCleanupMarker() {
        MockEnvironment environment = ownedEnvironment();
        environment.setProperty("app.test.cleanup.enabled", "false");
        assertThatThrownBy(() -> guard.verifyRedis(environment)).hasMessageContaining("owned test-container");
    }

    @Test
    void refusesCleanupWithoutOwnershipMarker() {
        MockEnvironment environment = ownedEnvironment();
        environment.setProperty("app.test.container.owned", "false");
        assertThatThrownBy(() -> guard.verifyRedis(environment)).hasMessageContaining("owned test-container");
    }

    @Test
    void refusesRedisCleanupWhenApplicationEndpointDiffersFromOwnedEndpoint() {
        MockEnvironment environment = ownedEnvironment();
        environment.setProperty("spring.data.redis.port", "26379");
        assertThatThrownBy(() -> guard.verifyRedis(environment)).hasMessageContaining("Redis not owned");
    }

    @Test
    void acceptsRedisCleanupForExactOwnedEndpoint() {
        guard.verifyRedis(ownedEnvironment());
    }

    @Test
    void refusesDatabaseCleanupForWrongCatalog() throws Exception {
        assertThatThrownBy(() -> guard.verifyDatabase(ownedEnvironment(),
                dataSource("production", "jdbc:mysql://mysql-host:33060/production")))
                .hasMessageContaining("database not owned");
    }

    @Test
    void refusesDatabaseCleanupWhenJdbcEndpointDiffersFromOwnedEndpoint() throws Exception {
        assertThatThrownBy(() -> guard.verifyDatabase(ownedEnvironment(),
                dataSource("gnilc_auth_test", "jdbc:mysql://other-host:33060/gnilc_auth_test")))
                .hasMessageContaining("database not owned");
    }

    @Test
    void acceptsDatabaseCleanupForExactCatalogAndOwnedEndpoint() throws Exception {
        guard.verifyDatabase(ownedEnvironment(),
                dataSource("gnilc_auth_test", "jdbc:mysql://mysql-host:33060/gnilc_auth_test"));
    }

    private MockEnvironment ownedEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.test.cleanup.enabled", "true")
                .withProperty("app.test.container.owned", "true")
                .withProperty("spring.data.redis.host", "redis-host")
                .withProperty("spring.data.redis.port", "16379")
                .withProperty("app.test.container.redis.host", "redis-host")
                .withProperty("app.test.container.redis.port", "16379")
                .withProperty("app.test.container.mysql.host", "mysql-host")
                .withProperty("app.test.container.mysql.port", "33060");
        environment.setActiveProfiles("test");
        return environment;
    }

    private DataSource dataSource(String catalog, String jdbcUrl) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn(catalog);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn(jdbcUrl);
        return dataSource;
    }
}
