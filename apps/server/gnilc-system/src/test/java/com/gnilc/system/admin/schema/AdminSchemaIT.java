package com.gnilc.system.admin.schema;

import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.test.container.MySqlContainerContextInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = MySqlContainerContextInitializer.class)
class AdminSchemaIT {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createRbacSchema() {
        dropSchema();
        runScript("sql/schema/01_rbac.sql");
    }

    @Test
    void adminSchemaCreatesTableAndDefaultAdminRelations() {
        runScript("sql/schema/02_admin.sql");

        assertThat(tableNames()).contains("sys_admin");
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(count("az_role", "code = 'admin' AND del = 0 AND built_in = 1")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user u
                  JOIN sys_admin a ON a.user_id = u.id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND u.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
    }

    @Test
    void adminSchemaCanRunRepeatedlyAndRestoreDefaultRelations() {
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/02_admin.sql");

        assertThat(count("az_role", "code = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user u
                  JOIN sys_admin a ON a.user_id = u.id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND u.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
        Long userId = jdbc.queryForObject(
                "SELECT user_id FROM sys_admin WHERE username = 'admin'", Long.class);

        jdbc.update("""
                DELETE ur
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND r.code = 'admin'
                """);
        runScript("sql/schema/02_admin.sql");

        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);

        jdbc.update("DELETE FROM az_user WHERE id = ?", userId);
        runScript("sql/schema/02_admin.sql");
        assertThat(count("az_user", "id = " + userId + " AND del = 0")).isEqualTo(1);

        jdbc.update("UPDATE az_user_role SET del = 1 WHERE user_id = ?", userId);
        jdbc.update("""
                UPDATE sys_admin
                   SET del = 1,
                       password = 'operator-managed-hash',
                       nickname = 'Operator Managed',
                       avatar = 'https://example.test/avatar.png',
                       description = 'Operator managed description',
                       home_path = '/operator-home',
                       status = 0
                 WHERE username = 'admin'
                """);
        jdbc.update("UPDATE az_user SET del = 1 WHERE id = ?", userId);
        jdbc.update("UPDATE az_role SET del = 1, built_in = 0 WHERE code = 'admin'");

        runScript("sql/schema/02_admin.sql");

        assertThat(count("az_role", "code = 'admin' AND del = 0 AND built_in = 1")).isEqualTo(1);
        assertThat(count("az_role", "code = 'admin'")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin'")).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT password, nickname, avatar, description, home_path, status
                  FROM sys_admin
                 WHERE username = 'admin'
                """))
                .containsEntry("password", "operator-managed-hash")
                .containsEntry("nickname", "Operator Managed")
                .containsEntry("avatar", "https://example.test/avatar.png")
                .containsEntry("description", "Operator managed description")
                .containsEntry("home_path", "/operator-home")
                .containsEntry("status", true);
        assertThat(count("az_user", "id = " + userId + " AND del = 0")).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
    }

    @Test
    void adminPermissionsCanRunRepeatedly() {
        runScript("sql/schema/05_admin_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(11);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = name
                   AND code = CONCAT(target_qualifier, ':', target_identifier)
                   AND public_access = 1
                """, Integer.class)).isEqualTo(11);

        jdbc.update("""
                UPDATE az_permission
                   SET name = 'Operator managed',
                       public_access = 0
                 WHERE code = 'POST:/sys/admin/remove/{id}'
                """);
        runScript("sql/schema/05_admin_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(11);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = 'POST:/sys/admin/remove/{id}'
                   AND name = 'Operator managed'
                   AND public_access = 0
                """, Integer.class)).isEqualTo(1);
    }

    private java.util.List<String> tableNames() {
        return jdbc.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = database()
                """, String.class);
    }

    private int count(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
    }

    private int defaultAdminRoleBindingCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND r.code = 'admin'
                   AND r.del = 0
                   AND ur.del = 0
                """, Integer.class);
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    sys_admin,
                    az_role_menu,
                    az_role_permission,
                    az_user_role,
                    az_menu,
                    az_permission,
                    az_user,
                    az_role
                """);
    }
}
