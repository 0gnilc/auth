package com.gnilc.system.i18n.schema;

import com.gnilc.system.support.SystemTestApplication;
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
@ContextConfiguration(classes = SystemTestApplication.class)
@Testcontainers
@SuppressWarnings("resource")
class I18nMessageSchemaIT {

    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("gnilc_i18n_schema_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createBaseSchema() {
        dropSchema();
        runScript("sql/schema/01_rbac.sql");
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/05_admin_permissions.sql");
    }

    @Test
    void i18nMessageSchemaIsIdempotentAndSeedsMenusRolesAndPermissions() {
        runScript("sql/schema/06_i18n.sql");
        runScript("sql/schema/06_i18n.sql");

        assertThat(jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                 ORDER BY ordinal_position
                """, String.class)).containsExactly(
                        "id", "client", "message_key", "locale", "i18n_value",
                        "create_time", "update_time");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.statistics
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                   AND index_name = 'uk_message_key_locale_client'
                 ORDER BY seq_in_index
                """, String.class)).containsExactly("message_key", "locale", "client");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_i18n WHERE client = 'admin'", Integer.class))
                .isEqualTo(8);
        assertThat(jdbc.queryForList("""
                SELECT title FROM az_menu
                 WHERE name IN ('Dashboard', 'Profile', 'System', 'I18nMessage') AND del = 0
                 ORDER BY name
                """, String.class)).containsExactlyInAnyOrder(
                        "menu.dashboard.title",
                        "menu.profile.title",
                        "menu.system.title",
                        "menu.i18nMessage.title");
        assertThat(jdbc.queryForObject("""
                SELECT component FROM az_menu
                 WHERE name = 'System' AND del = 0
                """, String.class)).isEqualTo("BasicLayout");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM az_role
                 WHERE code = 'i18n-manager' AND del = 0 AND built_in = 1
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND r.code = 'i18n-manager'
                   AND ur.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM az_permission
                 WHERE code LIKE 'POST:/sys/i18n-message/%' AND public_access = 0
                """, Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'admin'
                   AND p.code = 'POST:/sys/i18n-message/bundle'
                   AND rp.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'i18n-manager'
                   AND p.code IN (
                       'POST:/sys/i18n-message/page',
                       'POST:/sys/i18n-message/values/{messageKey}',
                       'POST:/sys/i18n-message/save',
                       'POST:/sys/i18n-message/remove/{messageKey}')
                   AND rp.del = 0
                """, Integer.class)).isEqualTo(4);
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    sys_i18n,
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
