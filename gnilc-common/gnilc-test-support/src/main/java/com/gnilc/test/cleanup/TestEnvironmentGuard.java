package com.gnilc.test.cleanup;

import com.gnilc.test.container.MySqlContainerSupport;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 破坏性测试数据清理的环境安全守卫。
 * <p>
 * 守卫要求启用 {@code test} Profile、显式清理开关和容器归属标记，
 * 数据库会核对实际 JDBC 连接，Redis 会核对 Spring 连接配置与测试容器登记地址。
 */
public class TestEnvironmentGuard {
    /**
     * 验证数据源连接的是当前测试进程拥有的 MySQL 容器和固定测试 Schema。
     *
     * @param environment Spring 测试环境
     * @param dataSource 待执行清理的数据源
     * @throws IllegalStateException 环境标记、Schema 或连接地址不匹配时抛出
     */
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

    /**
     * 验证 Spring Redis 连接配置与当前测试进程登记的 Redis 容器地址一致。
     *
     * @param environment Spring 测试环境
     * @throws IllegalStateException 环境标记或连接地址不匹配时抛出
     */
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

    /**
     * 校验所有破坏性清理操作共享的 Profile、开关和容器归属标记。
     */
    private void verifyCommon(Environment environment) {
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        boolean cleanupEnabled = environment.getProperty("app.test.cleanup.enabled", Boolean.class, false);
        boolean containerOwned = environment.getProperty("app.test.container.owned", Boolean.class, false);
        if (!testProfile || !cleanupEnabled || !containerOwned) {
            throw new IllegalStateException("Destructive cleanup requires an owned test-container environment");
        }
    }

    /**
     * 将 JDBC URL 转换为可比较主机和端口的 URI。
     */
    private URI jdbcEndpoint(String jdbcUrl) {
        try {
            return URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid JDBC URL", e);
        }
    }
}
