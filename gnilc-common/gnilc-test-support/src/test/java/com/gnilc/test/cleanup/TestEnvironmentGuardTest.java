package com.gnilc.test.cleanup;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestEnvironmentGuardTest {
    @Test
    void cleanupRequiresProfileFlagAndExactContainerDatabase() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.test.cleanup.enabled", "true");
        environment.setActiveProfiles("test");
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("gnilc_auth_test");
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:mysql://localhost:3307/gnilc_auth_test?useSSL=false");
        LettuceConnectionFactory redis = redis("localhost", 6380, 0);

        assertThatCode(() -> guard(environment, dataSource, redis).assertCleanupAllowed())
                .doesNotThrowAnyException();
    }

    @Test
    void cleanupRejectsMissingProfileFlagOrUnexpectedDatabase() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("access");
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:mysql://shared:3306/access");
        LettuceConnectionFactory redis = redis("shared", 6379, 0);

        assertThatThrownBy(() -> guard(
                new MockEnvironment().withProperty("app.test.cleanup.enabled", "true"), dataSource, redis)
                .assertCleanupAllowed())
                .hasMessageContaining("test profile");

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        assertThatThrownBy(() -> guard(environment, dataSource, redis).assertCleanupAllowed())
                .hasMessageContaining("cleanup.enabled");

        environment.setProperty("app.test.cleanup.enabled", "true");
        assertThatThrownBy(() -> guard(environment, dataSource, redis).assertCleanupAllowed())
                .hasMessageContaining("database access");
    }

    @Test
    void cleanupRejectsNonContainerMysqlOrRedisEndpoints() throws Exception {
        MockEnvironment environment = new MockEnvironment().withProperty("app.test.cleanup.enabled", "true");
        environment.setActiveProfiles("test");
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("gnilc_auth_test");
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:mysql://shared:3306/gnilc_auth_test");

        assertThatThrownBy(() -> guard(environment, dataSource, redis("localhost", 6380, 0))
                .assertCleanupAllowed())
                .hasMessageContaining("non-container MySQL");

        when(metadata.getURL()).thenReturn("jdbc:mysql://localhost:3307/gnilc_auth_test");
        assertThatThrownBy(() -> guard(environment, dataSource, redis("shared", 6379, 0))
                .assertCleanupAllowed())
                .hasMessageContaining("non-container Redis");
    }

    private TestEnvironmentGuard guard(MockEnvironment environment,
                                       DataSource dataSource,
                                       LettuceConnectionFactory redis) {
        return new TestEnvironmentGuard(
                environment,
                dataSource,
                redis,
                "jdbc:mysql://localhost:3307/gnilc_auth_test",
                "localhost",
                6380,
                0);
    }

    private LettuceConnectionFactory redis(String host, int port, int database) {
        LettuceConnectionFactory connectionFactory = mock(LettuceConnectionFactory.class);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        configuration.setDatabase(database);
        when(connectionFactory.getStandaloneConfiguration()).thenReturn(configuration);
        return connectionFactory;
    }
}
