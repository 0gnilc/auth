package com.gnilc.test.cleanup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 清空当前 MySQL 测试 Schema 中全部基础表的数据。
 * <p>
 * 调用方必须先通过 {@link TestEnvironmentGuard} 验证环境归属；本类只负责执行清理。
 */
public class DatabaseCleaner {
    private final DataSource dataSource;

    /**
     * 创建数据库清理器。
     *
     * @param dataSource 指向测试容器数据库的数据源
     */
    public DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 临时关闭外键检查，截断当前 Schema 的全部基础表，并确保恢复外键检查。
     *
     * @throws IllegalStateException 获取连接、查询表或执行清理 SQL 失败时抛出
     */
    public void clean() {
        try (Connection connection = dataSource.getConnection()) {
            List<String> tables = tableNames(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                try {
                    for (String table : tables) {
                        statement.execute("TRUNCATE TABLE `" + safeIdentifier(table) + "`");
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clean test database", e);
        }
    }

    private List<String> tableNames(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (var statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                """);
             var result = statement.executeQuery()) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        }
        return tables;
    }

    /**
     * 拒绝包含特殊字符的表名，避免动态拼接 TRUNCATE 语句时引入标识符注入。
     */
    private String safeIdentifier(String table) {
        if (!table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe table identifier: " + table);
        }
        return table;
    }
}
