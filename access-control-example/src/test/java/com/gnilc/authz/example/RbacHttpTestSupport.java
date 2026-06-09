package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.authz.rbac.common.constant.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class RbacHttpTestSupport {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    protected JsonNode postOk(String path, Object body) throws Exception {
        JsonNode json = postJson(path, body, 200);
        assertSuccess(json);
        return json;
    }

    protected JsonNode postJson(String path, Object body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post("/api" + path)
                .contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        return performJson(request, expectedStatus);
    }

    protected JsonNode postRaw(String path, String rawBody, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post("/api" + path)
                .contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON);
        if (rawBody != null) {
            request.content(rawBody);
        }
        return performJson(request, expectedStatus);
    }

    protected JsonNode postWithoutContentType(String path, String rawBody, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post("/api" + path)
                .contextPath("/api");
        if (rawBody != null) {
            request.content(rawBody);
        }
        return performJson(request, expectedStatus);
    }

    protected JsonNode postArgumentInvalid(String path, Object body, String expectedMessagePart) throws Exception {
        JsonNode json = postJson(path, body, 200);
        assertArgumentInvalid(json, expectedMessagePart);
        return json;
    }

    protected JsonNode postIllegalCondition(String path, Object body, String expectedMessagePart) throws Exception {
        JsonNode json = postJson(path, body, 200);
        assertIllegalCondition(json, expectedMessagePart);
        return json;
    }

    protected void assertSuccess(JsonNode json) {
        assertEnvelope(json, ResponseCode.SUCCESS, ResponseCode.SUCCESS.getMessage());
    }

    protected void assertArgumentInvalid(JsonNode json, String expectedMessagePart) {
        assertEnvelope(json, ResponseCode.ARGUMENT_INVALID, expectedMessagePart);
    }

    protected void assertIllegalCondition(JsonNode json, String expectedMessagePart) {
        assertEnvelope(json, ResponseCode.ILLEGAL_CONDITION, expectedMessagePart);
    }

    protected void assertError(JsonNode json, String expectedMessagePart) {
        assertEnvelope(json, ResponseCode.ERROR, expectedMessagePart);
    }

    protected void assertForbidden(JsonNode json) {
        assertThat(json.path("code").asInt()).isEqualTo(403);
        assertThat(json.path("message").asText()).isEqualTo("access denied");
        assertThat(json.path("data").isNull()).isTrue();
    }

    protected void assertEnvelope(JsonNode json, ResponseCode responseCode, String expectedMessagePart) {
        assertThat(json.path("code").asInt()).isEqualTo(responseCode.getCode());
        if (expectedMessagePart != null) {
            assertThat(json.path("message").asText()).contains(expectedMessagePart);
        }
    }

    protected JsonNode deleteOk(String path) throws Exception {
        MvcResult result = mockMvc.perform(delete("/api" + path)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertSuccess(json);
        return json;
    }

    protected JsonNode getJson(String path, String userId, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = get("/api" + path)
                .contextPath("/api");
        if (userId != null) {
            request.header("X-Access-User-Id", userId);
        }
        return performJson(request, expectedStatus);
    }

    protected long createRole(String code, String name) throws Exception {
        postOk("/authz/role/create", Map.of(
                "code", code,
                "name", name,
                "remark", "integration test"
        ));
        return firstIdByCode("/authz/role/list", code);
    }

    protected long createPermission(String code, String name, String targetIdentifier, boolean publicAccess) throws Exception {
        postOk("/authz/permission/create", Map.of(
                "code", code,
                "name", name,
                "targetIdentifier", targetIdentifier,
                "targetQualifier", "GET",
                "publicAccess", publicAccess,
                "remark", "integration test"
        ));
        return firstIdByCode("/authz/permission/list", code);
    }

    protected long createUser() throws Exception {
        return longValue(postOk("/example/users", Map.of()).path("data"));
    }

    protected long createCatalogMenu(String name, String path) throws Exception {
        postOk("/authz/menu/create", Map.of(
                "pid", 0,
                "type", "catalog",
                "name", name,
                "title", name,
                "path", path,
                "order", 1
        ));
        return findMenuIdByName(name);
    }

    protected long firstIdByCode(String listPath, String code) throws Exception {
        JsonNode data = postOk(listPath, Map.of("code", code)).path("data");
        assertThat(data).hasSize(1);
        return longValue(data.get(0).path("id"));
    }

    protected long findMenuIdByName(String name) throws Exception {
        JsonNode data = postOk("/authz/menu/tree", Map.of()).path("data");
        JsonNode found = findMenuByName(data, name);
        assertThat(found).as("menu named %s", name).isNotNull();
        return longValue(found.path("id"));
    }

    private JsonNode findMenuByName(JsonNode menus, String name) {
        for (JsonNode menu : menus) {
            if (name.equals(menu.path("name").asText())) {
                return menu;
            }
            JsonNode found = findMenuByName(menu.path("children"), name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    protected void updateRolePermission(long roleId, List<Long> permissionIds) throws Exception {
        postOk("/authz/role-permission/update", Map.of(
                "roleId", roleId,
                "permissionIds", permissionIds
        ));
    }

    protected void updateRoleMenu(long roleId, List<Long> menuIds) throws Exception {
        postOk("/authz/role-menu/update", Map.of(
                "roleId", roleId,
                "menuIds", menuIds
        ));
    }

    protected void updateUserRole(long userId, List<Long> roleIds) throws Exception {
        postOk("/authz/user-role/update", Map.of(
                "userId", userId,
                "roleIds", roleIds
        ));
    }

    protected void clearAllPermissionCache() throws Exception {
        postOk("/authz/permission/cache/clear-all", Map.of());
    }

    protected void cleanup(long userId, long roleId, long permissionId, long menuId) throws Exception {
        if (roleId > 0) {
            updateRolePermission(roleId, List.of());
            updateRoleMenu(roleId, List.of());
        }
        if (userId > 0) {
            updateUserRole(userId, List.of());
            deleteOk("/example/users/" + userId);
        }
        if (permissionId > 0) {
            postOk("/authz/permission/remove/" + permissionId, Map.of());
        }
        if (menuId > 0) {
            postOk("/authz/menu/remove/" + menuId, Map.of());
        }
        if (roleId > 0) {
            postOk("/authz/role/remove/" + roleId, Map.of());
        }
    }

    protected void cleanupQuietly(long userId, long roleId, long permissionId, long menuId) {
        if (roleId > 0) {
            runQuietly(() -> updateRolePermission(roleId, List.of()));
            runQuietly(() -> updateRoleMenu(roleId, List.of()));
        }
        if (userId > 0) {
            runQuietly(() -> updateUserRole(userId, List.of()));
            runQuietly(() -> deleteOk("/example/users/" + userId));
        }
        if (permissionId > 0) {
            runQuietly(() -> postOk("/authz/permission/remove/" + permissionId, Map.of()));
        }
        if (menuId > 0) {
            runQuietly(() -> postOk("/authz/menu/remove/" + menuId, Map.of()));
        }
        if (roleId > 0) {
            runQuietly(() -> postOk("/authz/role/remove/" + roleId, Map.of()));
        }
    }

    protected long longValue(JsonNode node) {
        if (node.isNumber()) {
            return node.longValue();
        }
        return Long.parseLong(node.asText());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void runQuietly(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
