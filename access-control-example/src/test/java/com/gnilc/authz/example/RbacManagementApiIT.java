package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
class RbacManagementApiIT extends RbacHttpTestSupport {

    /**
     * Verifies management APIs create, relate, update, and query RBAC objects.
     */
    @Test
    void manageRolePermissionMenuAndRelationsThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String roleCode = "it_role_" + suffix;
        String permissionCode = "it_permission_" + suffix;
        String menuName = "it_menu_" + suffix;
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        long menuId = 0L;
        try {
            userId = createUser();
            roleId = createRole(roleCode, "Integration Role " + suffix);
            permissionId = createPermission(permissionCode, "Integration Permission " + suffix, "/example/protected/**", false);
            menuId = createCatalogMenu(menuName, "/it/menu/" + suffix);

            updateRolePermission(roleId, List.of(permissionId));
            updateRoleMenu(roleId, List.of(menuId));
            updateUserRole(userId, List.of(roleId));

            // Verify relation list APIs expose the new bindings.
            JsonNode rolePermissionIds = postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data");
            JsonNode roleMenuIds = postOk("/authz/role-menu/list/" + roleId, Map.of()).path("data");
            JsonNode userRoleIds = postOk("/authz/user-role/list/" + userId, Map.of()).path("data");
            assertThat(rolePermissionIds).hasSize(1);
            assertThat(longValue(rolePermissionIds.get(0))).isEqualTo(permissionId);
            assertThat(roleMenuIds).hasSize(1);
            assertThat(longValue(roleMenuIds.get(0))).isEqualTo(menuId);
            assertThat(userRoleIds).hasSize(1);
            assertThat(longValue(userRoleIds.get(0))).isEqualTo(roleId);

            // Update each object and verify lookup APIs reflect the changes.
            postOk("/authz/role/update", Map.of(
                    "id", roleId,
                    "code", roleCode,
                    "name", "Integration Role Updated " + suffix,
                    "remark", "updated"
            ));
            postOk("/authz/permission/update", Map.of(
                    "id", permissionId,
                    "code", permissionCode,
                    "name", "Integration Permission Updated " + suffix,
                    "targetIdentifier", "/example/protected/**",
                    "targetQualifier", "GET",
                    "publicAccess", false,
                    "remark", "updated"
            ));
            postOk("/authz/menu/update", Map.of(
                    "id", menuId,
                    "pid", 0,
                    "type", "catalog",
                    "name", menuName,
                    "title", "Integration Menu Updated " + suffix,
                    "path", "/it/menu/" + suffix,
                    "order", 2
            ));

            assertThat(postOk("/authz/role/page", Map.of("code", roleCode, "currentPage", 1, "pageSize", 10))
                    .path("data").path("list")).hasSize(1);
            assertThat(postOk("/authz/permission/list", Map.of("code", permissionCode))
                    .path("data").get(0).path("name").asText()).contains("Updated");
            assertThat(findMenuIdByName(menuName)).isEqualTo(menuId);
        } finally {
            cleanup(userId, roleId, permissionId, menuId);
        }
    }
}
