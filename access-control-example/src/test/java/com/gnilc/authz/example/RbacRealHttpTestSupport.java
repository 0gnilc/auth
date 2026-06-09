package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.authz.rbac.common.constant.ResponseCode;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

abstract class RbacRealHttpTestSupport {
    private static final Object SCENARIO_FILE_LOCK = new Object();
    private static final AtomicBoolean SCENARIO_FILE_INITIALIZED = new AtomicBoolean();
    private static final ThreadLocal<HttpResult> LAST_RESULT = new ThreadLocal<>();

    @LocalServerPort
    protected int port;
    @Autowired
    protected TestRestTemplate restTemplate;
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Resets the real HTTP scenario file before tests run.
     */
    @BeforeAll
    static void resetScenarioFile() throws IOException {
        if (!SCENARIO_FILE_INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        Path file = scenarioFile();
        Files.createDirectories(file.getParent());
        Files.deleteIfExists(file);
    }

    protected JsonNode postOk(String path, Object body) throws Exception {
        JsonNode json = postJson(path, body, 200);
        assertSuccess(json);
        return json;
    }

    protected JsonNode postJson(String path, Object body, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchangeJson(HttpMethod.POST, path, new HttpEntity<>(body, headers), expectedStatus);
    }

    protected JsonNode postRaw(String path, String rawBody, MediaType contentType, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        return exchangeJson(HttpMethod.POST, path, new HttpEntity<>(rawBody, headers), expectedStatus);
    }

    protected JsonNode postRaw(String path, String rawBody, int expectedStatus) throws Exception {
        return postRaw(path, rawBody, MediaType.APPLICATION_JSON, expectedStatus);
    }

    protected JsonNode postWithoutContentType(String path, String rawBody, int expectedStatus) throws Exception {
        return postRaw(path, rawBody, null, expectedStatus);
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

    protected JsonNode getJson(String path, String userId, int expectedStatus) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (userId != null) {
            headers.set("X-Access-User-Id", userId);
        }
        return exchangeJson(HttpMethod.GET, path, new HttpEntity<>(headers), expectedStatus);
    }

    protected JsonNode deleteOk(String path) throws Exception {
        JsonNode json = exchangeJson(HttpMethod.DELETE, path, new HttpEntity<>(new HttpHeaders()), 200);
        assertSuccess(json);
        return json;
    }

    protected long createUser() throws Exception {
        return longValue(postOk("/example/users", Map.of()).path("data"));
    }

    protected long createRole(String code, String name) throws Exception {
        postOk("/authz/role/create", body(
                "code", code,
                "name", name,
                "remark", "real http integration test"
        ));
        return firstIdByCode("/authz/role/list", code);
    }

    protected long createPermission(String code, String name, String targetIdentifier, boolean publicAccess) throws Exception {
        return createPermission(code, name, targetIdentifier, "GET", publicAccess);
    }

    protected long createPermission(String code, String name, String targetIdentifier, String targetQualifier, boolean publicAccess) throws Exception {
        postOk("/authz/permission/create", body(
                "code", code,
                "name", name,
                "targetIdentifier", targetIdentifier,
                "targetQualifier", targetQualifier,
                "publicAccess", publicAccess,
                "remark", "real http integration test"
        ));
        return firstIdByCode("/authz/permission/list", code);
    }

    protected long createCatalogMenu(String name, String path) throws Exception {
        return createMenu(body(
                "pid", 0,
                "type", "catalog",
                "name", name,
                "title", name,
                "path", path,
                "order", 1
        ));
    }

    protected long createMenu(Map<String, Object> menuBody) throws Exception {
        postOk("/authz/menu/create", menuBody);
        return findMenuIdByName((String) menuBody.get("name"));
    }

    protected void updateRolePermission(long roleId, List<Long> permissionIds) throws Exception {
        postOk("/authz/role-permission/update", body("roleId", roleId, "permissionIds", permissionIds));
    }

    protected void updateRoleMenu(long roleId, List<Long> menuIds) throws Exception {
        postOk("/authz/role-menu/update", body("roleId", roleId, "menuIds", menuIds));
    }

    protected void updateUserRole(long userId, List<Long> roleIds) throws Exception {
        postOk("/authz/user-role/update", body("userId", userId, "roleIds", roleIds));
    }

    protected void clearAllPermissionCache() throws Exception {
        postOk("/authz/permission/cache/clear-all", Map.of());
    }

    protected long firstIdByCode(String listPath, String code) throws Exception {
        JsonNode data = postOk(listPath, body("code", code)).path("data");
        assertThat(data).hasSize(1);
        return longValue(data.get(0).path("id"));
    }

    protected long findMenuIdByName(String name) throws Exception {
        JsonNode data = postOk("/authz/menu/tree", Map.of()).path("data");
        JsonNode found = findMenuByName(data, name);
        assertThat(found).as("menu named %s", name).isNotNull();
        return longValue(found.path("id"));
    }

    protected JsonNode findMenuByName(JsonNode menus, String name) {
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

    protected void assertProtectedPingAllowed(long userId) throws Exception {
        JsonNode json = getJson("/example/protected/ping", String.valueOf(userId), 200);
        assertSuccess(json);
        assertThat(json.path("data").path("message").asText()).isEqualTo("protected pong");
    }

    protected void assertProtectedPingDenied(String userId) throws Exception {
        assertForbidden(getJson("/example/protected/ping", userId, 403));
    }

    protected void assertPublicPingAllowed(String userId) throws Exception {
        JsonNode json = getJson("/example/public/ping", userId, 200);
        assertSuccess(json);
        assertThat(json.path("data").path("message").asText()).isEqualTo("public pong");
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

    protected void recordScenario(String featureInterface, String input, String operation, String expectedResult) throws Exception {
        HttpResult result = LAST_RESULT.get();
        assertThat(result).as("last HTTP result before recording scenario").isNotNull();
        recordScenario(featureInterface, input, operation, expectedResult, result.summary());
    }

    protected void recordScenario(String featureInterface, String input, String operation,
                                  String expectedResult, String actualOutputResult) throws Exception {
        ScenarioDocRow row = new ScenarioDocRow(featureInterface, input, operation, expectedResult, actualOutputResult);
        synchronized (SCENARIO_FILE_LOCK) {
            Path file = scenarioFile();
            Files.createDirectories(file.getParent());
            Files.writeString(file, objectMapper.writeValueAsString(row) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    protected void cleanupQuietly(long userId, List<Long> roleIds, List<Long> permissionIds, List<Long> menuIds) {
        for (Long roleId : roleIds) {
            if (roleId != null && roleId > 0) {
                runQuietly(() -> updateRolePermission(roleId, List.of()));
                runQuietly(() -> updateRoleMenu(roleId, List.of()));
            }
        }
        if (userId > 0) {
            runQuietly(() -> updateUserRole(userId, List.of()));
            runQuietly(() -> deleteOk("/example/users/" + userId));
        }
        for (Long permissionId : permissionIds) {
            if (permissionId != null && permissionId > 0) {
                runQuietly(() -> postOk("/authz/permission/remove/" + permissionId, Map.of()));
            }
        }
        for (Long menuId : menuIds) {
            if (menuId != null && menuId > 0) {
                runQuietly(() -> postOk("/authz/menu/remove/" + menuId, Map.of()));
            }
        }
        for (Long roleId : roleIds) {
            if (roleId != null && roleId > 0) {
                runQuietly(() -> postOk("/authz/role/remove/" + roleId, Map.of()));
            }
        }
    }

    protected long longValue(JsonNode node) {
        if (node.isNumber()) {
            return node.longValue();
        }
        return Long.parseLong(node.asText());
    }

    protected Map<String, Object> body(Object... pairs) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            body.put((String) pairs[i], pairs[i + 1]);
        }
        return body;
    }

    protected String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private JsonNode exchangeJson(HttpMethod method, String path, HttpEntity<?> entity, int expectedStatus) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url(path), method, entity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        String body = Objects.requireNonNullElse(response.getBody(), "null");
        JsonNode json = objectMapper.readTree(body);
        LAST_RESULT.set(new HttpResult(response.getStatusCode().value(), objectMapper.writeValueAsString(json)));
        return json;
    }

    private static Path scenarioFile() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (cwd.getFileName() != null && "access-control-example".equals(cwd.getFileName().toString())) {
            return cwd.resolve("target/rbac-real-http-scenarios.jsonl");
        }
        return cwd.resolve("access-control-example/target/rbac-real-http-scenarios.jsonl");
    }

    private String compactJson(String body) {
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(body));
        } catch (Exception ignored) {
            return body;
        }
    }

    private void runQuietly(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
        }
    }

    private record HttpResult(int status, String body) {
        String summary() {
            return "HTTP " + status + "; " + compactBody(body);
        }

        private static String compactBody(String body) {
            if (body == null) {
                return "";
            }
            return body.replace('\n', ' ').replace('\r', ' ').trim();
        }
    }

    private record ScenarioDocRow(String featureInterface, String input, String operation,
                                  String expectedResult, String actualOutputResult) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
