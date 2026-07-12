package com.gnilc.test.cleanup;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 清空当前 MySQL schema 中的业务表，同时保留数据库迁移工具的元数据表。
 */
public final class DatabaseCleaner {
    private static final Set<String> PRESERVED_TABLES = Set.of(
            "flyway_schema_history", "databasechangelog", "databasechangeloglock");
    private static final List<String> PRESERVED_PREFIXES = List.of("qrtz_", "undo_log");

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate 连接测试 MySQL 的 JDBC 操作入口
     */
    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 暂时关闭外键检查并截断当前 schema 的全部业务表，结束前恢复外键检查。
     */
    public void truncateBusinessTables() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_type = 'BASE TABLE'
                """, String.class).stream()
                .filter(this::isBusinessTable)
                .sorted()
                .toList();
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : tables) {
                    statement.addBatch("TRUNCATE TABLE " + quote(table));
                }
                statement.executeBatch();
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
    }

    private boolean isBusinessTable(String table) {
        String name = table.toLowerCase(Locale.ROOT);
        return !PRESERVED_TABLES.contains(name)
                && PRESERVED_PREFIXES.stream().noneMatch(name::startsWith);
    }

    private String quote(String identifier) {
        char quote = 96;
        return quote + identifier.replace(String.valueOf(quote), String.valueOf(quote) + quote) + quote;
    }
}
