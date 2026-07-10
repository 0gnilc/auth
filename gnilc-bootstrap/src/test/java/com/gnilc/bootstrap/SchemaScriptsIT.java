package com.gnilc.bootstrap;

import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.test.container.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SchemaScriptsIT {
    @Test
    void createsExpectedSchemaAndKeepsAdminSeedIdempotent() throws Exception {
        try (MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse(
                System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
                .withDatabaseName(MySqlContainerSupport.DATABASE_NAME)
                .withUsername("test")
                .withPassword("test")) {
            mysql.start();
            try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/schema/01-rbac.sql"));
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/schema/02-admin.sql"));
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/schema/02-admin.sql"));

                Set<String> tables = tableNames(connection);
                assertThat(tables).containsExactlyInAnyOrder(
                        "az_role", "az_permission", "az_menu", "az_user",
                        "az_user_role", "az_role_permission", "az_role_menu", "sys_admin");

                assertIndex(connection, "az_role", "uk_code", true, "code");
                assertIndex(connection, "az_permission", "idx_target_identifier", false, "target_identifier");
                assertIndex(connection, "az_user_role", "idx_user_role", false, "user_id", "role_id");
                assertIndex(connection, "sys_admin", "uk_username", true, "username");
                assertIndex(connection, "sys_admin", "uk_user_id", true, "user_id");
                assertColumn(connection, "az_permission", "target_identifier", "varchar", 500);
                assertColumn(connection, "az_permission", "public_access", "bit", null);
                assertColumn(connection, "sys_admin", "password", "varchar", 100);
                assertColumn(connection, "sys_admin", "status", "bit", null);

                try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM sys_admin WHERE username = 'admin' AND del = 0");
                     var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isEqualTo(1);
                }
            }
        }
    }

    private void assertIndex(Connection connection, String table, String index, boolean unique,
                             String... columns) throws Exception {
        var indexedColumns = new java.util.TreeMap<Short, String>();
        Boolean nonUnique = null;
        try (var result = connection.getMetaData().getIndexInfo(
                MySqlContainerSupport.DATABASE_NAME, null, table, false, false)) {
            while (result.next()) {
                if (index.equals(result.getString("INDEX_NAME"))) {
                    indexedColumns.put(result.getShort("ORDINAL_POSITION"), result.getString("COLUMN_NAME"));
                    nonUnique = result.getBoolean("NON_UNIQUE");
                }
            }
        }
        assertThat(indexedColumns.values()).containsExactly(columns);
        assertThat(nonUnique).isEqualTo(!unique);
    }

    private void assertColumn(Connection connection, String table, String column,
                              String dataType, Integer length) throws Exception {
        try (var result = connection.getMetaData().getColumns(
                MySqlContainerSupport.DATABASE_NAME, null, table, column)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("TYPE_NAME")).isEqualToIgnoringCase(dataType);
            if (length != null) {
                assertThat(result.getInt("COLUMN_SIZE")).isEqualTo(length);
            }
            assertThat(result.next()).isFalse();
        }
    }

    private Set<String> tableNames(Connection connection) throws Exception {
        Set<String> names = new java.util.LinkedHashSet<>();
        try (var result = connection.getMetaData().getTables(
                MySqlContainerSupport.DATABASE_NAME, null, "%", new String[]{"TABLE"})) {
            while (result.next()) {
                names.add(result.getString("TABLE_NAME"));
            }
        }
        return names;
    }
}
