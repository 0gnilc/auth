package com.gnilc.test.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 进程级 MySQL Testcontainers 生命周期与动态属性支持。
 * <p>
 * 默认启动 {@code mysql:8.4.0}，使用不可复用容器和固定测试 Schema。
 * 可通过系统属性 {@code app.test.mysql.image} 覆盖镜像。
 */
public final class MySqlContainerSupport {
    /** 允许执行破坏性清理的固定测试 Schema 名称。 */
    public static final String DATABASE_NAME = "gnilc_auth_test";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static final String MARKER_PROPERTY = "app.test.container.owned";
    // 进程级单例由 Testcontainers 的 Ryuk 在 JVM 退出后回收，不能在单次调用后关闭。
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> CONTAINER = new MySQLContainer<>(mysqlImage())
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withReuse(false);
    private static final Set<String> INITIALIZED_SCRIPTS = new LinkedHashSet<>();

    private MySqlContainerSupport() {
    }

    /**
     * 获取当前进程共享的 MySQL 容器，并在首次访问时启动。
     *
     * @return 已启动的 MySQL 测试容器
     */
    public static synchronized MySQLContainer<?> container() {
        startContainer();
        return CONTAINER;
    }

    /**
     * 启动 MySQL 容器并输出 Spring DataSource、清理开关和容器归属属性。
     *
     * @param properties 属性键值接收器
     */
    public static void applyProperties(BiConsumer<String, Object> properties) {
        startContainer();
        applyProperties(properties, CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword(),
                CONTAINER.getDriverClassName(), CONTAINER.getHost(),
                CONTAINER.getMappedPort(MySQLContainer.MYSQL_PORT));
    }

    /**
     * 根据给定连接信息组装属性；该重载供无容器单元测试验证属性契约。
     */
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

    /**
     * 按调用顺序执行模块声明的类路径 SQL 脚本，每个脚本在当前进程中最多执行一次。
     * <p>
     * 通用容器支持不决定业务 Schema；脚本列表由使用该容器的业务模块测试提供。
     *
     * @param classpathScripts 需要执行的类路径 SQL 脚本
     * @throws IllegalStateException 无法连接数据库时抛出
     * @throws org.springframework.jdbc.datasource.init.ScriptException SQL 脚本读取或执行失败时抛出
     */
    public static synchronized void initializeSchema(String... classpathScripts) {
        startContainer();
        try (Connection connection = DriverManager.getConnection(
                CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword())) {
            for (String script : classpathScripts) {
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

    private static synchronized void startContainer() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
    }

    private static DockerImageName mysqlImage() {
        return DockerImageName.parse(System.getProperty("app.test.mysql.image", "mysql:8.4.0"));
    }
}
