package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("localtest")
class RbacRealHttpBoundaryIT extends RbacRealHttpTestSupport {

    /**
     * Verifies real HTTP management boundary cases return stable responses.
     */
    @Test
    void managementBoundaryCasesAreStableThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        long protectedPermissionId = 0L;
        long menuId = 0L;
        try {
            // Exercise management validation envelopes first.
            postArgumentInvalid("/authz/role/create", Map.of(), "请输入角色标识");
            roleId = createRole("it_real_role_" + suffix, "Real HTTP Role " + suffix);
            postArgumentInvalid("/authz/role/create", body(
                    "code", "it_real_role_" + suffix,
                    "name", "Duplicate Real HTTP Role " + suffix
            ), "角色标识已存在");
            JsonNode page = postOk("/authz/role/page", body(
                    "code", "it_real_role_" + suffix,
                    "currentPage", 0,
                    "pageSize", -1
            )).path("data");
            assertThat(page.path("currentPage").asLong()).isEqualTo(1L);
            assertThat(page.path("pageSize").asLong()).isEqualTo(10L);

            postArgumentInvalid("/authz/permission/create", body(
                    "code", "it_real_permission_missing_target_" + suffix,
                    "name", "Missing Target " + suffix,
                    "targetIdentifier", "  "
            ), "请输入访问目标标识");
            permissionId = createPermission("it_real_permission_" + suffix, "Real HTTP Permission " + suffix,
                    "/real/http/" + suffix, false);
            postArgumentInvalid("/authz/permission/create", body(
                    "code", "it_real_permission_" + suffix,
                    "name", "Duplicate Real HTTP Permission " + suffix,
                    "targetIdentifier", "/real/http/duplicate/" + suffix
            ), "权限标识已存在");
            postOk("/authz/permission/cache/clear-all", Map.of());

            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "name", "it_real_missing_type_" + suffix,
                    "title", "Missing Type " + suffix,
                    "path", "/real/http/menu/missing-type/" + suffix
            ), "请选择菜单类型");
            String menuName = "it_real_menu_" + suffix;
            String menuPath = "/real/http/menu/" + suffix;
            menuId = createCatalogMenu(menuName, menuPath);
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "catalog",
                    "name", "it_real_duplicate_path_" + suffix,
                    "title", "Duplicate Path " + suffix,
                    "path", menuPath
            ), "路由路径已存在");

            // Relation updates should deduplicate and clear predictably.
            postOk("/authz/role-permission/update", body(
                    "roleId", roleId,
                    "permissionIds", List.of(permissionId, permissionId)
            ));
            JsonNode relationIds = postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data");
            assertThat(relationIds).hasSize(1);
            assertThat(longValue(relationIds.get(0))).isEqualTo(permissionId);
            postOk("/authz/role-permission/update", body("roleId", roleId, "permissionIds", null));
            assertThat(postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data")).isEmpty();

            // Malformed requests are normalized to stable envelopes.
            assertArgumentInvalid(postRaw("/authz/role/create", "{\"code\":", MediaType.APPLICATION_JSON, 200), "请求体格式错误");
            assertArgumentInvalid(postJson("/authz/role/remove/not-a-number", Map.of(), 200), "请求参数格式错误");
            assertArgumentInvalid(postRaw("/authz/role/create", "{}", null, 200), "请求内容类型不支持");

            // Irregular identities should remain unauthorized.
            protectedPermissionId = createPermission("it_real_identity_protected_" + suffix,
                    "Real HTTP Identity Protected " + suffix,
                    "/example/protected/**",
                    false);
            postOk("/authz/permission/cache/clear-all", Map.of());
            assertForbidden(getJson("/example/protected/ping", "   ", 403));
            assertForbidden(getJson("/example/protected/ping", "not-a-user", 403));
            userId = createUser();
            assertForbidden(getJson("/example/protected/ping", String.valueOf(userId), 403));
        } finally {
            cleanupQuietly(userId, roleId, protectedPermissionId, menuId);
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }
}
