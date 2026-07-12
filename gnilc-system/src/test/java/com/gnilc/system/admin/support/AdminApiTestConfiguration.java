package com.gnilc.system.admin.support;

import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class AdminApiTestConfiguration {
    static final String LIMITED_USERNAME = "limited";
    static final String LIMITED_PASSWORD = "123456";
    private static final long LIMITED_USER_ID = 900_001L;
    private static final String DEFAULT_PASSWORD_HASH =
            "$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G";
    private static final List<String> PROTECTED_PATHS = List.of(
            "/sys/admin/user-info",
            "/sys/admin/role-codes",
            "/sys/admin/menu/access-codes",
            "/sys/admin/page",
            "/sys/admin/create",
            "/sys/admin/update",
            "/sys/admin/update-roles",
            "/sys/admin/remove/**"
    );

    @Bean
    BaselineDataSeeder adminApiBaselineDataSeeder(DataSource dataSource, JdbcTemplate jdbc) {
        return () -> {
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/02_admin.sql"))
                    .execute(dataSource);
            Long adminRoleId = jdbc.queryForObject(
                    "select id from az_role where code = 'admin' and del = 0", Long.class);
            jdbc.update("""
                    insert into az_role (del, create_time, code, name, built_in)
                    values (0, now(), 'limited', 'Limited', 0)
                    """);
            Long limitedRoleId = jdbc.queryForObject(
                    "select id from az_role where code = 'limited' and del = 0", Long.class);
            jdbc.update("insert into az_user (id, del, create_time) values (?, 0, now())", LIMITED_USER_ID);
            jdbc.update("""
                    insert into sys_admin
                        (del, create_time, user_id, username, password, nickname, home_path, status)
                    values (0, now(), ?, ?, ?, 'Limited', '/workspace', 1)
                    """, LIMITED_USER_ID, LIMITED_USERNAME, DEFAULT_PASSWORD_HASH);
            jdbc.update("""
                    insert into az_user_role (del, create_time, user_id, role_id)
                    values (0, now(), ?, ?)
                    """, LIMITED_USER_ID, limitedRoleId);
            for (int i = 0; i < PROTECTED_PATHS.size(); i++) {
                String code = "system:admin:" + i;
                jdbc.update("""
                        insert into az_permission
                            (del, create_time, code, name, target_identifier, public_access)
                        values (0, now(), ?, ?, ?, 0)
                        """, code, code, PROTECTED_PATHS.get(i));
                Long permissionId = jdbc.queryForObject(
                        "select id from az_permission where code = ?", Long.class, code);
                jdbc.update("""
                        insert into az_role_permission
                            (del, create_time, role_id, permission_id)
                        values (0, now(), ?, ?)
                        """, adminRoleId, permissionId);
            }
            jdbc.update("""
                    insert into az_menu
                        (del, create_time, pid, type, status, access_code, name, title, `order`)
                    values (0, now(), 0, 'button', 1, 'admin:manage', 'admin-manage',
                            'Admin management', 1)
                    """);
            Long menuId = jdbc.queryForObject(
                    "select id from az_menu where access_code = 'admin:manage'", Long.class);
            jdbc.update("""
                    insert into az_role_menu
                        (del, create_time, role_id, menu_id)
                    values (0, now(), ?, ?)
                    """, adminRoleId, menuId);
        };
    }
}
