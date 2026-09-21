package com.gnilc.core.admin.support;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class AdminApiTestConfiguration {
    static final String LIMITED_USERNAME = "limited";
    static final String LIMITED_PASSWORD = "123456";
    private static final long LIMITED_USER_ID = 900_001L;
    private static final String DEFAULT_PASSWORD_HASH =
            "$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G";

    public static void grantDefaultAdminRoles(JdbcTemplate jdbc, String... roleCodes) {
        for (String roleCode : roleCodes) {
            jdbc.update("""
                    INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
                    SELECT 0, UTC_TIMESTAMP(6), NULL, admin.user_id, role.id
                      FROM sys_admin admin
                      JOIN az_role role ON role.code = ? AND role.del = 0
                     WHERE admin.username = 'admin'
                       AND admin.del = 0
                       AND NOT EXISTS (
                           SELECT 1
                             FROM az_user_role binding
                            WHERE binding.user_id = admin.user_id
                              AND binding.role_id = role.id
                              AND binding.del = 0
                       )
                    """, roleCode);
            int activeBindingCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                      FROM sys_admin admin
                      JOIN az_user_role binding
                        ON binding.user_id = admin.user_id
                       AND binding.del = 0
                      JOIN az_role role
                        ON role.id = binding.role_id
                       AND role.del = 0
                     WHERE admin.username = 'admin'
                       AND admin.del = 0
                       AND role.code = ?
                    """, Integer.class, roleCode);
            if (activeBindingCount != 1) {
                throw new IllegalStateException(
                        "Expected one active default Admin Role binding for " + roleCode
                                + ", found " + activeBindingCount);
            }
        }
    }

    private static void retainDefaultAdminTestRoles(JdbcTemplate jdbc) {
        jdbc.update("""
                DELETE binding
                  FROM az_user_role binding
                  JOIN sys_admin admin
                    ON admin.user_id = binding.user_id
                   AND admin.username = 'admin'
                   AND admin.del = 0
                  JOIN az_role role
                    ON role.id = binding.role_id
                 WHERE role.code NOT IN ('admin', 'rbac:manager')
                """);
        grantDefaultAdminRoles(jdbc, "rbac:manager");
    }

    @Bean
    BaselineDataSeeder adminApiBaselineDataSeeder(DataSource dataSource,
                                                  JdbcTemplate jdbc,
                                                  PermissionCacheService cacheService) {
        return () -> {
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/02_admin.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/03_framework_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/04_rbac_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/05_admin_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/06_i18n.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/07_rbac_admin.sql"))
                    .execute(dataSource);














            retainDefaultAdminTestRoles(jdbc);
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
            cacheService.resetAll();
        };
    }
}
