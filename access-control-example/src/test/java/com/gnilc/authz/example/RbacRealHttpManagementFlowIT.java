package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("localtest")
class RbacRealHttpManagementFlowIT extends RbacRealHttpTestSupport {

    /**
     * Verifies role create, update, page, remove, and list behavior.
     */
    @Test
    void roleLifecycleIsVisibleThroughRealHttpManagementApis() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String roleCode = "it_mgmt_role_" + suffix;
        long roleId = 0L;
        try {
            roleId = createRole(roleCode, "Management Role " + suffix);
            JsonNode created = postOk("/authz/role/list", body("code", roleCode)).path("data").get(0);
            assertThat(created.path("name").asText()).isEqualTo("Management Role " + suffix);

            postOk("/authz/role/update", body(
                    "id", roleId,
                    "code", roleCode + "_updated",
                    "name", "Management Role Updated " + suffix,
                    "remark", "updated through real http"
            ));
            JsonNode updatedPage = postOk("/authz/role/page", body(
                    "code", roleCode + "_updated",
                    "currentPage", 1,
                    "pageSize", 10
            )).path("data");
            assertThat(updatedPage.path("list")).hasSize(1);
            assertThat(updatedPage.path("list").get(0).path("name").asText()).isEqualTo("Management Role Updated " + suffix);

            postOk("/authz/role/remove/" + roleId, Map.of());
            roleId = 0L;
            assertThat(postOk("/authz/role/list", body("code", roleCode + "_updated")).path("data")).isEmpty();
            recordScenario("Management / /authz/role lifecycle",
                    "role code create=" + roleCode + "; then update code=" + roleCode + "_updated; remove by id",
                    "POST /api/authz/role/create -> list -> update -> page -> remove -> list",
                    "HTTP 200 envelopes throughout; role is created, updated, paged, removed, then absent from list");
        } finally {
            cleanupQuietly(0L, roleId, 0L, 0L);
        }
    }

    /**
     * Verifies permission create, update, list, remove, and absence behavior.
     */
    @Test
    void permissionLifecycleIsVisibleThroughRealHttpManagementApis() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String permissionCode = "it_mgmt_permission_" + suffix;
        long permissionId = 0L;
        try {
            permissionId = createPermission(permissionCode, "Management Permission " + suffix,
                    "/management/permission/" + suffix, false);
            JsonNode created = postOk("/authz/permission/list", body("code", permissionCode)).path("data").get(0);
            assertThat(created.path("targetIdentifier").asText()).isEqualTo("/management/permission/" + suffix);
            assertThat(created.path("publicAccess").asBoolean()).isFalse();

            postOk("/authz/permission/update", body(
                    "id", permissionId,
                    "code", permissionCode + "_updated",
                    "name", "Management Permission Updated " + suffix,
                    "targetIdentifier", "/management/permission/updated/" + suffix,
                    "targetQualifier", "POST",
                    "publicAccess", true,
                    "remark", "updated through real http"
            ));
            JsonNode updated = postOk("/authz/permission/list", body(
                    "code", permissionCode + "_updated",
                    "publicAccess", true
            )).path("data");
            assertThat(updated).hasSize(1);
            assertThat(updated.get(0).path("targetQualifier").asText()).isEqualTo("POST");

            postOk("/authz/permission/remove/" + permissionId, Map.of());
            permissionId = 0L;
            assertThat(postOk("/authz/permission/list", body("code", permissionCode + "_updated")).path("data")).isEmpty();
            recordScenario("Management / /authz/permission lifecycle",
                    "permission code create=" + permissionCode + "; update target/publicAccess/qualifier; remove by id",
                    "POST /api/authz/permission/create -> list -> update -> list -> remove -> list",
                    "HTTP 200 envelopes throughout; permission is created, updated, removed, then absent from list");
        } finally {
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies invalid role page values fall back to defaults.
     */
    @Test
    void rolePageFallsBackToDefaultPaginationForInvalidPageValuesThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long roleId = 0L;
        try {
            String roleCode = "it_mgmt_page_role_" + suffix;
            roleId = createRole(roleCode, "Management Page Role " + suffix);

            JsonNode page = postOk("/authz/role/page", body(
                    "code", roleCode,
                    "currentPage", 0,
                    "pageSize", -100
            )).path("data");
            assertThat(page.path("currentPage").asLong()).isEqualTo(1L);
            assertThat(page.path("pageSize").asLong()).isEqualTo(10L);
            assertThat(page.path("list")).hasSize(1);
            recordScenario("Management / POST /authz/role/page",
                    "body={code:" + roleCode + ", currentPage:0, pageSize:-100}",
                    "POST /api/authz/role/page",
                    "HTTP 200; code=200; currentPage defaults to 1 and pageSize defaults to 10");
        } finally {
            cleanupQuietly(0L, roleId, 0L, 0L);
        }
    }

    /**
     * Verifies role-permission replacement, deduplication, and clearing.
     */
    @Test
    void rolePermissionReplaceDeduplicateEmptyAndNullClearThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long roleId = 0L;
        long permissionIdOne = 0L;
        long permissionIdTwo = 0L;
        try {
            roleId = createRole("it_mgmt_rp_role_" + suffix, "Management Role Permission Role " + suffix);
            permissionIdOne = createPermission("it_mgmt_rp_permission_one_" + suffix,
                    "Role Permission One " + suffix, "/management/rp/one/" + suffix, false);
            permissionIdTwo = createPermission("it_mgmt_rp_permission_two_" + suffix,
                    "Role Permission Two " + suffix, "/management/rp/two/" + suffix, false);

            // Replace, deduplicate, and clear role-permission relations.
            updateRolePermission(roleId, List.of(permissionIdOne));
            assertSingleId("/authz/role-permission/list/" + roleId, permissionIdOne);
            updateRolePermission(roleId, List.of(permissionIdTwo, permissionIdTwo));
            assertSingleId("/authz/role-permission/list/" + roleId, permissionIdTwo);
            updateRolePermission(roleId, List.of());
            assertThat(postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data")).isEmpty();
            postOk("/authz/role-permission/update", body("roleId", roleId, "permissionIds", null));
            assertThat(postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data")).isEmpty();
            recordScenario("Management relation / /authz/role-permission",
                    "roleId=" + roleId + "; permissionIds add/replace/duplicate/empty/null",
                    "POST /api/authz/role-permission/update and POST /api/authz/role-permission/list/{roleId}",
                    "HTTP 200; relation list replaces previous values, deduplicates duplicate IDs, and clears for empty/null lists");
        } finally {
            cleanupQuietly(0L, roleId, permissionIdOne, 0L);
            cleanupQuietly(0L, 0L, permissionIdTwo, 0L);
        }
    }

    /**
     * Verifies user-role replacement, deduplication, and clearing.
     */
    @Test
    void userRoleReplaceDeduplicateEmptyAndNullClearThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleIdOne = 0L;
        long roleIdTwo = 0L;
        try {
            userId = createUser();
            roleIdOne = createRole("it_mgmt_ur_role_one_" + suffix, "User Role One " + suffix);
            roleIdTwo = createRole("it_mgmt_ur_role_two_" + suffix, "User Role Two " + suffix);

            // Replace, deduplicate, and clear user-role relations.
            updateUserRole(userId, List.of(roleIdOne));
            assertSingleId("/authz/user-role/list/" + userId, roleIdOne);
            updateUserRole(userId, List.of(roleIdTwo, roleIdTwo));
            assertSingleId("/authz/user-role/list/" + userId, roleIdTwo);
            updateUserRole(userId, List.of());
            assertThat(postOk("/authz/user-role/list/" + userId, Map.of()).path("data")).isEmpty();
            postOk("/authz/user-role/update", body("userId", userId, "roleIds", null));
            assertThat(postOk("/authz/user-role/list/" + userId, Map.of()).path("data")).isEmpty();
            recordScenario("Management relation / /authz/user-role",
                    "userId=" + userId + "; roleIds add/replace/duplicate/empty/null",
                    "POST /api/authz/user-role/update and POST /api/authz/user-role/list/{userId}",
                    "HTTP 200; relation list replaces previous values, deduplicates duplicate IDs, and clears for empty/null lists");
        } finally {
            cleanupQuietly(userId, List.of(roleIdOne, roleIdTwo), List.of(), List.of());
        }
    }

    /**
     * Verifies role-menu replacement, deduplication, and clearing.
     */
    @Test
    void roleMenuReplaceDeduplicateEmptyAndNullClearThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long roleId = 0L;
        long menuIdOne = 0L;
        long menuIdTwo = 0L;
        try {
            roleId = createRole("it_mgmt_rm_role_" + suffix, "Role Menu Role " + suffix);
            menuIdOne = createCatalogMenu("it_mgmt_rm_menu_one_" + suffix, "/management/role-menu/one/" + suffix);
            menuIdTwo = createCatalogMenu("it_mgmt_rm_menu_two_" + suffix, "/management/role-menu/two/" + suffix);

            // Replace, deduplicate, and clear role-menu relations.
            updateRoleMenu(roleId, List.of(menuIdOne));
            assertSingleId("/authz/role-menu/list/" + roleId, menuIdOne);
            updateRoleMenu(roleId, List.of(menuIdTwo, menuIdTwo));
            assertSingleId("/authz/role-menu/list/" + roleId, menuIdTwo);
            updateRoleMenu(roleId, List.of());
            assertThat(postOk("/authz/role-menu/list/" + roleId, Map.of()).path("data")).isEmpty();
            postOk("/authz/role-menu/update", body("roleId", roleId, "menuIds", null));
            assertThat(postOk("/authz/role-menu/list/" + roleId, Map.of()).path("data")).isEmpty();
            recordScenario("Management relation / /authz/role-menu",
                    "roleId=" + roleId + "; menuIds add/replace/duplicate/empty/null",
                    "POST /api/authz/role-menu/update and POST /api/authz/role-menu/list/{roleId}",
                    "HTTP 200; relation list replaces previous values, deduplicates duplicate IDs, and clears for empty/null lists");
        } finally {
            cleanupQuietly(0L, List.of(roleId), List.of(), List.of(menuIdTwo, menuIdOne));
        }
    }

    /**
     * Verifies missing entities return stable management error envelopes.
     */
    @Test
    void missingEntitiesReturnStableManagementErrorEnvelopesThroughRealHttp() throws Exception {
        postIllegalCondition("/authz/role/remove/-1", Map.of(), "角色不存在，请刷新后重试");
        recordScenario("Management error / POST /authz/role/remove/{id}",
                "path id=-1",
                "POST /api/authz/role/remove/-1",
                "HTTP 200; envelope code=410; role does not exist message");

        postIllegalCondition("/authz/permission/remove/-1", Map.of(), "权限不存在，请刷新后重试");
        recordScenario("Management error / POST /authz/permission/remove/{id}",
                "path id=-1",
                "POST /api/authz/permission/remove/-1",
                "HTTP 200; envelope code=410; permission does not exist message");

        assertArgumentInvalid(postJson("/authz/menu/remove/-1", Map.of(), 200), "菜单不存在，请刷新后重试");
        recordScenario("Management error / POST /authz/menu/remove/{id}",
                "path id=-1",
                "POST /api/authz/menu/remove/-1",
                "HTTP 200; envelope code=400; menu does not exist message");
    }

    private void assertSingleId(String listPath, long expectedId) throws Exception {
        JsonNode ids = postOk(listPath, Map.of()).path("data");
        assertThat(ids).hasSize(1);
        assertThat(longValue(ids.get(0))).isEqualTo(expectedId);
    }
}
