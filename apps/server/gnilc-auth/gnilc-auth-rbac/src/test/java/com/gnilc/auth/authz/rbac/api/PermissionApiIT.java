package com.gnilc.auth.authz.rbac.api;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@ApiTest
@Import(RestExceptionHandlingConfiguration.class)
@ContextConfiguration(
        classes = RbacTestApplication.class,
        initializers = RbacContainerContextInitializer.class)
class PermissionApiIT extends ApiTestSupport {

    @Autowired
    private PermissionService permissions;

    @Test
    void permissionHttpContractCreatesQueriesUpdatesAndRemoves() {
        post("/authz/permission/create", request(
                "audit:read", "Read audit records", "/audit/**"))
                .body("code", equalTo(0));
        PermissionBo permission = permissions.getPermissionByCode("audit:read");

        post("/authz/permission/list", Map.of("code", "audit:read"))
                .body("data[0].targetIdentifier", equalTo("/audit/**"));
        post("/authz/permission/update", Map.of(
                "id", permission.getId(),
                "code", "audit:read",
                "name", "Read audited records",
                "targetIdentifier", "/audited/**",
                "targetQualifier", "GET",
                "publicAccess", false))
                .body("code", equalTo(0));
        post("/authz/permission/list", Map.of("code", "audit:read"))
                .body("data[0].name", equalTo("Read audited records"))
                .body("data[0].targetIdentifier", equalTo("/audited/**"));

        post("/authz/permission/remove/" + permission.getId(), Map.of())
                .body("code", equalTo(0));
        post("/authz/permission/cache/clear-all", Map.of())
                .body("code", equalTo(0));
        post("/authz/permission/list", Map.of("code", "audit:read"))
                .body("data", empty());
    }

    @Test
    void permissionBoundariesRejectOverflowAndKeepMaximumUnicodeValuesReusable() {
        post("/authz/permission/create", request(
                "oversized:permission",
                "Oversized permission",
                "/" + "p".repeat(500)))
                .body("code", equalTo(10001));
        assertThat(permissions.getPermissionByCode("oversized:permission")).isNull();

        String code = "p".repeat(255);
        Map<String, Object> request = Map.of(
                "code", code,
                "name", "\uD83D\uDE00".repeat(255),
                "targetIdentifier", "/" + "t".repeat(499),
                "targetQualifier", "q".repeat(100),
                "remark", "m".repeat(500),
                "publicAccess", false);
        post("/authz/permission/create", request).body("code", equalTo(0));
        post("/authz/permission/remove/" + permissions.getPermissionByCode(code).getId(), Map.of())
                .body("code", equalTo(0));
        post("/authz/permission/create", request).body("code", equalTo(0));
    }

    private Map<String, Object> request(String code, String name, String targetIdentifier) {
        return Map.of(
                "code", code,
                "name", name,
                "targetIdentifier", targetIdentifier,
                "targetQualifier", "GET",
                "publicAccess", false);
    }

    private io.restassured.response.ValidatableResponse post(String path, Object body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(path)
                .then()
                .statusCode(200);
    }
}
