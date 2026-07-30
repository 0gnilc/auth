package com.gnilc.auth.authz.rbac.schema;

import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(classes = RbacTestApplication.class)
@Testcontainers
@SuppressWarnings("resource")
class RbacSchemaIT {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("gnilc_rbac_schema_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    az_role_menu,
                    az_role_permission,
                    az_user_role,
                    az_menu,
                    az_permission,
                    az_user,
                    az_role
                """);
    }

    @Test
    void rbacSchemaCreatesAllCurrentTables() {
        runScript();

        assertThat(tableNames()).contains(
                "az_role",
                "az_permission",
                "az_menu",
                "az_user",
                "az_user_role",
                "az_role_permission",
                "az_role_menu");
        assertThat(columnLength("az_role", "code")).isEqualTo(320);
        assertThat(columnLength("az_permission", "code")).isEqualTo(320);
        assertThat(columnLength("az_menu", "access_code")).isEqualTo(320);
        assertThat(columnLength("az_menu", "name")).isEqualTo(320);
        assertThat(columnLength("az_menu", "path")).isEqualTo(560);
    }

    @Test
    void rbacSchemaCanRunRepeatedly() {
        runScript();
        runScript();

        assertThat(tableNames()).contains(
                "az_role",
                "az_permission",
                "az_menu",
                "az_user",
                "az_user_role",
                "az_role_permission",
                "az_role_menu");
    }

    @Test
    void frameworkAndRbacPermissionsCanRunRepeatedly() {
        runScript();
        runScript("sql/schema/03_framework_permissions.sql");
        runScript("sql/schema/04_rbac_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(21);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = name
                   AND code = CONCAT(target_qualifier, ':', target_identifier)
                   AND public_access = 1
                """, Integer.class)).isEqualTo(21);

        jdbc.update("""
                UPDATE az_permission
                   SET name = 'Operator managed',
                       public_access = 0
                 WHERE code = 'POST:/authz/menu/create'
                """);
        runScript("sql/schema/03_framework_permissions.sql");
        runScript("sql/schema/04_rbac_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(21);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = 'POST:/authz/menu/create'
                   AND name = 'Operator managed'
                   AND public_access = 0
                """, Integer.class)).isEqualTo(1);
    }

    private java.util.List<String> tableNames() {
        return jdbc.queryForList("""
                select table_name
                  from information_schema.tables
                 where table_schema = database()
                """, String.class);
    }

    private Integer columnLength(String table, String column) {
        return jdbc.queryForObject("""
                SELECT character_maximum_length
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = ?
                   AND column_name = ?
                """, Integer.class, table, column);
    }

    private void runScript() {
        runScript("sql/schema/01_rbac.sql");
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }
}
