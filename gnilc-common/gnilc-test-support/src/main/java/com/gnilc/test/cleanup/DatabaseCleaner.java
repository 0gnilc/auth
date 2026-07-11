package com.gnilc.test.cleanup;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DatabaseCleaner {
    private static final Set<String> PRESERVED_TABLES = Set.of(
            "flyway_schema_history", "databasechangelog", "databasechangeloglock");

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void truncateBusinessTables() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_type = 'BASE TABLE'
                """, String.class).stream()
                .filter(table -> !PRESERVED_TABLES.contains(table.toLowerCase(Locale.ROOT)))
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

    private String quote(String identifier) {
        char quote = 96;
        return quote + identifier.replace(String.valueOf(quote), String.valueOf(quote) + quote) + quote;
    }
}
