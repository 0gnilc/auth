package com.gnilc.test.cleanup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseCleaner {
    private final DataSource dataSource;

    public DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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

    private String safeIdentifier(String table) {
        if (!table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe table identifier: " + table);
        }
        return table;
    }
}
