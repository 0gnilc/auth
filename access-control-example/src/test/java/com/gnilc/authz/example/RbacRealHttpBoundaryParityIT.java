package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("localtest")
class RbacRealHttpBoundaryParityIT extends RbacRealHttpTestSupport {

    /**
     * Verifies malformed JSON returns an argument-invalid envelope.
     */
    @Test
    void malformedJsonReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/create", "{\"code\":", 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role/create",
                "malformed JSON body={\"code\":",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies empty JSON bodies return an argument-invalid envelope.
     */
    @Test
    void emptyBodyReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postJson("/authz/role/create", null, 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role/create",
                "empty request body with Content-Type application/json",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies array bodies are rejected when an object is expected.
     */
    @Test
    void arrayBodyWhereObjectExpectedReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/create", "[]", 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role/create",
                "JSON array body [] where role object is expected",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies string bodies are rejected when an object is expected.
     */
    @Test
    void stringBodyWhereObjectExpectedReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/create", "\"not-an-object\"", 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role/create",
                "JSON string body \"not-an-object\" where role object is expected",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies wrong numeric field types return an argument-invalid envelope.
     */
    @Test
    void wrongNumericFieldTypeReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/page", "{\"currentPage\":\"abc\",\"pageSize\":10}", 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role/page",
                "body={currentPage:'abc', pageSize:10}",
                "POST /api/authz/role/page",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies wrong list element types return an argument-invalid envelope.
     */
    @Test
    void wrongListElementTypeReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role-permission/update",
                "{\"roleId\":1,\"permissionIds\":[\"abc\"]}", 200), "请求体格式错误");
        recordScenario("Boundary / POST /authz/role-permission/update",
                "body={roleId:1, permissionIds:['abc']}",
                "POST /api/authz/role-permission/update",
                "HTTP 200; envelope code=400; message contains 请求体格式错误");
    }

    /**
     * Verifies malformed path variables return an argument-invalid envelope.
     */
    @Test
    void badPathVariableTypeReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postJson("/authz/role/remove/not-a-number", Map.of(), 200), "请求参数格式错误");
        recordScenario("Boundary / POST /authz/role/remove/{id}",
                "path id=not-a-number",
                "POST /api/authz/role/remove/not-a-number",
                "HTTP 200; envelope code=400; message contains 请求参数格式错误");
    }

    /**
     * Verifies malformed relation-list path variables are rejected.
     */
    @Test
    void malformedRelationListPathVariableReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postJson("/authz/role-permission/list/not-a-number", Map.of(), 200), "请求参数格式错误");
        recordScenario("Boundary / POST /authz/role-permission/list/{roleId}",
                "path roleId=not-a-number",
                "POST /api/authz/role-permission/list/not-a-number",
                "HTTP 200; envelope code=400; message contains 请求参数格式错误");
    }

    /**
     * Verifies missing content type returns an argument-invalid envelope.
     */
    @Test
    void missingContentTypeReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postWithoutContentType("/authz/role/create", "{}", 200), "请求内容类型不支持");
        recordScenario("Boundary / POST /authz/role/create",
                "body={} without Content-Type header",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求内容类型不支持");
    }

    /**
     * Verifies unsupported content type returns an argument-invalid envelope.
     */
    @Test
    void unsupportedContentTypeReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/role/create", "{}", MediaType.TEXT_PLAIN, 200), "请求内容类型不支持");
        recordScenario("Boundary / POST /authz/role/create",
                "body={} with Content-Type text/plain",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; message contains 请求内容类型不支持");
    }

    /**
     * Verifies invalid menu enum values return an argument-invalid envelope.
     */
    @Test
    void invalidMenuEnumValueReturnsArgumentInvalidEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postRaw("/authz/menu/create",
                "{\"pid\":0,\"type\":\"spaceship\",\"name\":\"bad\",\"title\":\"bad\"}", 200),
                "请求体格式错误");
        recordScenario("Boundary / POST /authz/menu/create",
                "body={pid:0,type:'spaceship',name:'bad',title:'bad'}",
                "POST /api/authz/menu/create",
                "HTTP 200; envelope code=400; invalid enum is normalized to 请求体格式错误");
    }

    /**
     * Verifies missing relation owner IDs return argument-invalid envelopes.
     */
    @Test
    void missingRelationOwnerIdsReturnArgumentInvalidEnvelopesThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        long menuId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_boundary_relation_role_" + suffix, "Boundary Relation Role " + suffix);
            permissionId = createPermission("it_boundary_relation_permission_" + suffix,
                    "Boundary Relation Permission " + suffix, "/boundary/relation/" + suffix, false);
            menuId = createCatalogMenu("it_boundary_relation_menu_" + suffix, "/boundary/relation/menu/" + suffix);

            assertArgumentInvalid(postJson("/authz/role-permission/update", body("permissionIds", List.of(permissionId)), 200), "请选择角色");
            recordScenario("Boundary / POST /authz/role-permission/update",
                    "body={permissionIds:[" + permissionId + "]}; roleId omitted",
                    "POST /api/authz/role-permission/update",
                    "HTTP 200; envelope code=400; message contains 请选择角色");

            assertArgumentInvalid(postJson("/authz/role-menu/update", body("menuIds", List.of(menuId)), 200), "请选择角色");
            recordScenario("Boundary / POST /authz/role-menu/update",
                    "body={menuIds:[" + menuId + "]}; roleId omitted",
                    "POST /api/authz/role-menu/update",
                    "HTTP 200; envelope code=400; message contains 请选择角色");

            assertArgumentInvalid(postJson("/authz/user-role/update", body("roleIds", List.of(roleId)), 200), "请选择用户");
            recordScenario("Boundary / POST /authz/user-role/update",
                    "body={roleIds:[" + roleId + "]}; userId omitted",
                    "POST /api/authz/user-role/update",
                    "HTTP 200; envelope code=400; message contains 请选择用户");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, menuId);
        }
    }

    /**
     * Verifies whitespace-only business input is rejected by service validation.
     */
    @Test
    void whitespaceOnlyBusinessInputReturnsServiceValidationEnvelopeThroughRealHttp() throws Exception {
        assertArgumentInvalid(postJson("/authz/role/create", body(
                "code", "   ",
                "name", "Whitespace Role"
        ), 200), "请输入角色标识");
        recordScenario("Boundary / POST /authz/role/create",
                "body={code:'   ', name:'Whitespace Role'}",
                "POST /api/authz/role/create",
                "HTTP 200; envelope code=400; whitespace-only code is rejected by service validation");
    }
}
