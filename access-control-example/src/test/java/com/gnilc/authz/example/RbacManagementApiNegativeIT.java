package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
class RbacManagementApiNegativeIT extends RbacHttpTestSupport {

    /**
     * Verifies role creation rejects a missing code.
     */
    @Test
    void roleCreateRejectsMissingCodeThroughHttpApi() throws Exception {
        postArgumentInvalid("/authz/role/create", Map.of(), "请输入角色标识");
    }

    /**
     * Verifies role validation and missing-role envelopes.
     */
    @Test
    void roleManagementRejectsDuplicateBlankAndMissingRolesThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String roleCode = "it_negative_role_" + suffix;
        long roleId = 0L;
        try {
            postArgumentInvalid("/authz/role/create", body(
                    "code", "   ",
                    "name", "Blank Role " + suffix
            ), "请输入角色标识");

            roleId = createRole(roleCode, "Negative Role " + suffix);

            postArgumentInvalid("/authz/role/create", body(
                    "code", roleCode,
                    "name", "Duplicate Role " + suffix
            ), "角色标识已存在");
            postArgumentInvalid("/authz/role/update", body(
                    "code", roleCode,
                    "name", "Missing Id Role " + suffix
            ), "请选择角色");
            postIllegalCondition("/authz/role/update", body(
                    "id", -1L,
                    "code", "missing_role_" + suffix,
                    "name", "Missing Role " + suffix
            ), "角色不存在，请刷新后重试");
            postIllegalCondition("/authz/role/remove/-1", Map.of(), "角色不存在，请刷新后重试");

            JsonNode page = postOk("/authz/role/page", body(
                    "code", roleCode,
                    "currentPage", 0,
                    "pageSize", -1
            )).path("data");
            assertThat(page.path("currentPage").asLong()).isEqualTo(1L);
            assertThat(page.path("pageSize").asLong()).isEqualTo(10L);
            assertThat(page.path("list")).hasSize(1);
        } finally {
            cleanupQuietly(0L, roleId, 0L, 0L);
        }
    }

    /**
     * Verifies permission validation, duplicates, and missing-permission envelopes.
     */
    @Test
    void permissionManagementRejectsRequiredFieldsDuplicatesAndMissingPermissionsThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String permissionCode = "it_negative_permission_" + suffix;
        long permissionId = 0L;
        try {
            postArgumentInvalid("/authz/permission/create", body(
                    "code", permissionCode,
                    "targetIdentifier", "/negative/permission/" + suffix
            ), "请输入权限名称");
            postArgumentInvalid("/authz/permission/create", body(
                    "name", "Missing Code Permission " + suffix,
                    "targetIdentifier", "/negative/permission/" + suffix
            ), "请输入权限标识");
            postArgumentInvalid("/authz/permission/create", body(
                    "code", "it_missing_target_" + suffix,
                    "name", "Missing Target Permission " + suffix,
                    "targetIdentifier", "   "
            ), "请输入访问目标标识");

            permissionId = createPermission(permissionCode, "Negative Permission " + suffix, "/negative/permission/" + suffix, false);

            postArgumentInvalid("/authz/permission/create", body(
                    "code", permissionCode,
                    "name", "Duplicate Permission " + suffix,
                    "targetIdentifier", "/negative/permission/duplicate/" + suffix
            ), "权限标识已存在");
            postArgumentInvalid("/authz/permission/update", body(
                    "code", permissionCode,
                    "name", "Missing Id Permission " + suffix,
                    "targetIdentifier", "/negative/permission/" + suffix
            ), "请选择权限");
            postIllegalCondition("/authz/permission/update", body(
                    "id", -1L,
                    "code", "missing_permission_" + suffix,
                    "name", "Missing Permission " + suffix,
                    "targetIdentifier", "/negative/permission/missing/" + suffix
            ), "权限不存在，请刷新后重试");
            postIllegalCondition("/authz/permission/remove/-1", Map.of(), "权限不存在，请刷新后重试");

            assertThat(postOk("/authz/permission/list", body("code", "unknown_permission_" + suffix)).path("data")).isEmpty();
            assertThat(postOk("/authz/permission/list", body("publicAccess", true)).path("data")).isNotNull();
            clearAllPermissionCache();
        } finally {
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies menu required-field, type-specific, and uniqueness validation.
     */
    @Test
    void menuManagementRejectsRequiredTypeSpecificAndDuplicateFieldsThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long catalogId = 0L;
        long buttonId = 0L;
        try {
            // Check common menu validation before type-specific fields.
            postArgumentInvalid("/authz/menu/create", body(
                    "type", "catalog",
                    "name", "missing_pid_" + suffix,
                    "title", "Missing Pid " + suffix,
                    "path", "/negative/menu/missing-pid/" + suffix
            ), "请选择父菜单");
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", -1L,
                    "type", "catalog",
                    "name", "missing_parent_" + suffix,
                    "title", "Missing Parent " + suffix,
                    "path", "/negative/menu/missing-parent/" + suffix
            ), "父菜单不存在，请重新选择");
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "name", "missing_type_" + suffix,
                    "title", "Missing Type " + suffix,
                    "path", "/negative/menu/missing-type/" + suffix
            ), "请选择菜单类型");
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "catalog",
                    "title", "Missing Name " + suffix,
                    "path", "/negative/menu/missing-name/" + suffix
            ), "请输入菜单名称");
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "catalog",
                    "name", "missing_title_" + suffix,
                    "path", "/negative/menu/missing-title/" + suffix
            ), "请输入菜单标题");

            postArgumentInvalid("/authz/menu/create", menuBody(suffix, "catalog", "catalog_missing_path", "path", null), "请输入路由路径");
            postArgumentInvalid("/authz/menu/create", menuBody(suffix, "menu", "menu_missing_component", "component", null), "请输入页面组件");
            postArgumentInvalid("/authz/menu/create", menuBody(suffix, "button", "button_missing_access", "accessCode", null), "请输入权限标识");
            postArgumentInvalid("/authz/menu/create", menuBody(suffix, "embedded", "embedded_missing_iframe", "iframeSrc", null), "请输入内嵌页面地址");
            postArgumentInvalid("/authz/menu/create", menuBody(suffix, "link", "link_missing_link", "link", null), "请输入外链地址");

            String menuName = "it_negative_menu_" + suffix;
            String menuPath = "/negative/menu/" + suffix;
            catalogId = createCatalogMenu(menuName, menuPath);
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "catalog",
                    "name", menuName,
                    "title", "Duplicate Name " + suffix,
                    "path", "/negative/menu/duplicate-name/" + suffix
            ), "菜单名称已存在");
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "catalog",
                    "name", "duplicate_path_" + suffix,
                    "title", "Duplicate Path " + suffix,
                    "path", menuPath
            ), "路由路径已存在");

            // Button access codes must also remain unique.
            String accessCode = "negative:menu:button:" + suffix;
            String buttonName = "it_negative_button_" + suffix;
            postOk("/authz/menu/create", body(
                    "pid", 0,
                    "type", "button",
                    "name", buttonName,
                    "title", "Negative Button " + suffix,
                    "accessCode", accessCode,
                    "order", 1
            ));
            buttonId = findMenuIdByName(buttonName);
            postArgumentInvalid("/authz/menu/create", body(
                    "pid", 0,
                    "type", "button",
                    "name", "duplicate_access_" + suffix,
                    "title", "Duplicate Access " + suffix,
                    "accessCode", accessCode
            ), "权限标识已存在");

            postArgumentInvalid("/authz/menu/update", body(
                    "pid", 0,
                    "type", "catalog",
                    "name", "missing_menu_id_" + suffix,
                    "title", "Missing Menu Id " + suffix,
                    "path", "/negative/menu/missing-id/" + suffix
            ), "请选择菜单");
            postArgumentInvalid("/authz/menu/update", body(
                    "id", -1L,
                    "pid", 0,
                    "type", "catalog",
                    "name", "missing_menu_" + suffix,
                    "title", "Missing Menu " + suffix,
                    "path", "/negative/menu/missing/" + suffix
            ), "菜单不存在，请刷新后重试");
            postArgumentInvalid("/authz/menu/remove/-1", Map.of(), "菜单不存在，请刷新后重试");
        } finally {
            cleanupQuietly(0L, 0L, 0L, buttonId);
            cleanupQuietly(0L, 0L, 0L, catalogId);
        }
    }

    /**
     * Verifies relation APIs reject missing owners and normalize duplicate or null IDs.
     */
    @Test
    void relationUpdatesRejectMissingOwnersAndTreatDuplicateAndNullListsAsIdempotentThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        long menuId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_relation_role_" + suffix, "Relation Role " + suffix);
            permissionId = createPermission("it_relation_permission_" + suffix, "Relation Permission " + suffix,
                    "/negative/relation/" + suffix, false);
            menuId = createCatalogMenu("it_relation_menu_" + suffix, "/negative/relation/menu/" + suffix);

            postArgumentInvalid("/authz/role-permission/update", body("permissionIds", List.of(permissionId)), "请选择角色");
            postArgumentInvalid("/authz/role-menu/update", body("menuIds", List.of(menuId)), "请选择角色");
            postArgumentInvalid("/authz/user-role/update", body("roleIds", List.of(roleId)), "请选择用户");

            // Duplicate IDs collapse to one relation; null clears all.
            postOk("/authz/role-permission/update", body("roleId", roleId, "permissionIds", List.of(permissionId, permissionId)));
            JsonNode rolePermissionIds = postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data");
            assertThat(rolePermissionIds).hasSize(1);
            assertThat(longValue(rolePermissionIds.get(0))).isEqualTo(permissionId);
            postOk("/authz/role-permission/update", body("roleId", roleId, "permissionIds", null));
            assertThat(postOk("/authz/role-permission/list/" + roleId, Map.of()).path("data")).isEmpty();

            postOk("/authz/role-menu/update", body("roleId", roleId, "menuIds", List.of(menuId, menuId)));
            JsonNode roleMenuIds = postOk("/authz/role-menu/list/" + roleId, Map.of()).path("data");
            assertThat(roleMenuIds).hasSize(1);
            assertThat(longValue(roleMenuIds.get(0))).isEqualTo(menuId);
            postOk("/authz/role-menu/update", body("roleId", roleId, "menuIds", List.of()));
            assertThat(postOk("/authz/role-menu/list/" + roleId, Map.of()).path("data")).isEmpty();

            postOk("/authz/user-role/update", body("userId", userId, "roleIds", List.of(roleId, roleId)));
            JsonNode userRoleIds = postOk("/authz/user-role/list/" + userId, Map.of()).path("data");
            assertThat(userRoleIds).hasSize(1);
            assertThat(longValue(userRoleIds.get(0))).isEqualTo(roleId);
            postOk("/authz/user-role/update", body("userId", userId, "roleIds", null));
            assertThat(postOk("/authz/user-role/list/" + userId, Map.of()).path("data")).isEmpty();
        } finally {
            cleanupQuietly(userId, roleId, permissionId, menuId);
        }
    }

    /**
     * Verifies malformed HTTP inputs return stable argument-invalid envelopes.
     */
    @Test
    void malformedHttpInputsReturnStableArgumentInvalidEnvelopeThroughHttpApi() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/create", "{\"code\":", 200), "请求体格式错误");
        assertArgumentInvalid(postJson("/authz/role/create", null, 200), "请求体格式错误");
        assertArgumentInvalid(postRaw("/authz/role/create", "[]", 200), "请求体格式错误");
        assertArgumentInvalid(postRaw("/authz/role/create", "\"not-an-object\"", 200), "请求体格式错误");
        assertArgumentInvalid(postRaw("/authz/role/page", "{\"currentPage\":\"abc\",\"pageSize\":10}", 200), "请求体格式错误");
        assertArgumentInvalid(postRaw("/authz/role-permission/update", "{\"roleId\":1,\"permissionIds\":[\"abc\"]}", 200), "请求体格式错误");
        assertArgumentInvalid(postRaw("/authz/menu/create", "{\"pid\":0,\"type\":\"spaceship\",\"name\":\"bad\",\"title\":\"bad\"}", 200), "请求体格式错误");
        assertArgumentInvalid(postJson("/authz/role/remove/not-a-number", Map.of(), 200), "请求参数格式错误");
        assertArgumentInvalid(postJson("/authz/role-permission/list/not-a-number", Map.of(), 200), "请求参数格式错误");
        assertArgumentInvalid(postWithoutContentType("/authz/role/create", "{}", 200), "请求内容类型不支持");
    }

    /**
     * Verifies irregular identity headers do not gain protected access.
     */
    @Test
    void protectedEndpointTreatsIrregularIdentityHeadersAsAnonymousThroughHttpApi() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long protectedPermissionId = 0L;
        try {
            protectedPermissionId = createPermission(
                    "it_identity_protected_" + suffix,
                    "Identity Protected " + suffix,
                    "/example/protected/**",
                    false);
            clearAllPermissionCache();

            assertForbidden(getJson("/example/protected/ping", "   ", 403));
            assertForbidden(getJson("/example/protected/ping", "not-a-user", 403));

            userId = createUser();
            assertForbidden(getJson("/example/protected/ping", String.valueOf(userId), 403));
        } finally {
            cleanupQuietly(userId, 0L, protectedPermissionId, 0L);
        }
    }

    private Map<String, Object> menuBody(String suffix, String type, String namePart, String missingField, Object missingValue) {
        Map<String, Object> body = body(
                "pid", 0,
                "type", type,
                "name", namePart + "_" + suffix,
                "title", "Menu " + namePart + " " + suffix,
                "path", "/negative/menu/" + namePart + "/" + suffix,
                "component", "/negative/menu/component/" + suffix,
                "accessCode", "negative:menu:" + namePart + ":" + suffix,
                "iframeSrc", "https://example.test/embed/" + suffix,
                "link", "https://example.test/link/" + suffix,
                "order", 1
        );
        body.put(missingField, missingValue);
        return body;
    }

    private Map<String, Object> body(Object... pairs) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            body.put((String) pairs[i], pairs[i + 1]);
        }
        return body;
    }
}
