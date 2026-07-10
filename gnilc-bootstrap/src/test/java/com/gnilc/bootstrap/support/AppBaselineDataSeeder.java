package com.gnilc.bootstrap.support;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class AppBaselineDataSeeder implements BaselineDataSeeder {
    public static final String ADMIN_USERNAME = "test-admin";
    public static final String ADMIN_PASSWORD = "TestAdmin1!";
    public static final String LIMITED_USERNAME = "test-limited";
    public static final String LIMITED_PASSWORD = "TestLimited1!";

    private final JdbcTemplate jdbcTemplate;
    private final PermissionCache permissionCache;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AppBaselineDataSeeder(JdbcTemplate jdbcTemplate, PermissionCache permissionCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionCache = permissionCache;
    }

    @Override
    public void seed() {
        long adminRole = insertRole("test-admin", "Test Administrator");
        long limitedRole = insertRole("test-limited", "Test Limited");
        long adminUser = insertAdmin(ADMIN_USERNAME, ADMIN_PASSWORD, "Test Administrator");
        long limitedUser = insertAdmin(LIMITED_USERNAME, LIMITED_PASSWORD, "Test Limited");

        jdbcTemplate.update("INSERT INTO az_user_role (del, create_time, user_id, role_id) VALUES (0, NOW(), ?, ?)", adminUser, adminRole);
        jdbcTemplate.update("INSERT INTO az_user_role (del, create_time, user_id, role_id) VALUES (0, NOW(), ?, ?)", limitedUser, limitedRole);

        publicPermission("admin:login", "/sys/admin/login");
        publicPermission("admin:refresh", "/sys/admin/refresh");
        publicPermission("admin:logout", "/sys/admin/logout");
        rolePermission(adminRole, "admin:user-info", "/sys/admin/user-info");
        rolePermission(adminRole, "admin:role-codes", "/sys/admin/role-codes");
        rolePermission(adminRole, "admin:menu-codes", "/sys/admin/menu/access-codes");
        rolePermission(adminRole, "admin:page", "/sys/admin/page");
        rolePermission(adminRole, "admin:create", "/sys/admin/create");
        rolePermission(adminRole, "admin:update", "/sys/admin/update");
        rolePermission(adminRole, "admin:update-roles", "/sys/admin/update-roles");
        rolePermission(adminRole, "admin:remove", "/sys/admin/remove/**");

        jdbcTemplate.update("""
                INSERT INTO az_menu (
                    del, create_time, pid, type, status, access_code, name, `order`, title
                ) VALUES (0, NOW(), 0, 'button', 1, 'admin:create', 'test-admin-create', 1, 'Create administrator')
                """);
        long menuId = jdbcTemplate.queryForObject("SELECT id FROM az_menu WHERE name = 'test-admin-create'", Long.class);
        jdbcTemplate.update("INSERT INTO az_role_menu (del, create_time, role_id, menu_id) VALUES (0, NOW(), ?, ?)", adminRole, menuId);
        verifyAdminBaseline(adminUser, adminRole, ADMIN_USERNAME);
        verifyAdminBaseline(limitedUser, limitedRole, LIMITED_USERNAME);
        permissionCache.resetAll();
    }

    private long insertRole(String code, String name) {
        jdbcTemplate.update("INSERT INTO az_role (del, create_time, code, name, built_in) VALUES (0, NOW(), ?, ?, 0)", code, name);
        return jdbcTemplate.queryForObject("SELECT id FROM az_role WHERE code = ?", Long.class, code);
    }

    private long insertAdmin(String username, String password, String nickname) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO az_user (del, create_time) VALUES (0, NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (inserted != 1 || key == null) {
            throw new IllegalStateException("Failed to create baseline user for " + username);
        }
        long userId = key.longValue();
        jdbcTemplate.update("""
                INSERT INTO sys_admin (
                    del, create_time, user_id, username, password, nickname, home_path, status
                ) VALUES (0, NOW(), ?, ?, ?, ?, '/workspace', 1)
                """, userId, username, passwordEncoder.encode(password), nickname);
        return userId;
    }

    private void verifyAdminBaseline(long userId, long roleId, String username) {
        Integer linkedRows = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM az_user u
                JOIN sys_admin a ON a.user_id = u.id AND a.del = 0
                JOIN az_user_role ur ON ur.user_id = u.id AND ur.role_id = ? AND ur.del = 0
                WHERE u.id = ? AND u.del = 0 AND a.username = ?
                """, Integer.class, roleId, userId, username);
        if (linkedRows == null || linkedRows != 1) {
            throw new IllegalStateException("Invalid baseline links for " + username);
        }
    }

    private void publicPermission(String code, String path) {
        insertPermission(code, path, true);
    }

    private void rolePermission(long roleId, String code, String path) {
        long permissionId = insertPermission(code, path, false);
        jdbcTemplate.update("INSERT INTO az_role_permission (del, create_time, role_id, permission_id) VALUES (0, NOW(), ?, ?)", roleId, permissionId);
    }

    private long insertPermission(String code, String path, boolean publicAccess) {
        jdbcTemplate.update("""
                INSERT INTO az_permission (
                    del, create_time, code, name, target_identifier, public_access
                ) VALUES (0, NOW(), ?, ?, ?, ?)
                """, code, code, path, publicAccess);
        return jdbcTemplate.queryForObject("SELECT id FROM az_permission WHERE code = ?", Long.class, code);
    }
}
