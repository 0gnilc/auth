package com.gnilc.auth.authz.rbac.api;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.service.RoleService;
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
class RoleApiIT extends ApiTestSupport {

    @Autowired
    private RoleService roles;

    @Test
    void roleHttpContractCreatesQueriesUpdatesAndRemoves() {
        post("/authz/role/create", Map.of(
                "code", "auditor",
                "name", "Auditor",
                "remark", "Read-only audit role"))
                .body("code", equalTo(0));
        RoleBo role = roles.getRoleByCode("auditor");

        post("/authz/role/list", Map.of("code", "auditor"))
                .body("data[0].code", equalTo("auditor"))
                .body("data[0].createTime", equalTo(role.getCreateTime().toString()));
        post("/authz/role/page", Map.of(
                "code", "auditor",
                "currentPage", 1,
                "pageSize", 10))
                .body("data.totalCount", equalTo(1));
        post("/authz/role/update", Map.of(
                "id", role.getId(),
                "code", "audit-reviewer",
                "name", "Audit Reviewer"))
                .body("code", equalTo(0));
        post("/authz/role/list", Map.of("code", "audit-reviewer"))
                .body("data[0].name", equalTo("Audit Reviewer"));

        post("/authz/role/remove/" + role.getId(), Map.of())
                .body("code", equalTo(0));
        post("/authz/role/list", Map.of("code", "audit-reviewer"))
                .body("data", empty());
    }

    @Test
    void roleBoundariesRejectOverflowAndKeepMaximumUnicodeValuesReusable() {
        post("/authz/role/create", Map.of(
                "code", "r".repeat(256),
                "name", "Oversized role"))
                .body("code", equalTo(10001));
        assertThat(roles.getRoleByCode("r".repeat(256))).isNull();

        String roleCode = "r".repeat(255);
        Map<String, Object> request = Map.of(
                "code", roleCode,
                "name", "\uD83D\uDE00".repeat(255),
                "remark", "m".repeat(500));
        post("/authz/role/create", request).body("code", equalTo(0));
        post("/authz/role/remove/" + roles.getRoleByCode(roleCode).getId(), Map.of())
                .body("code", equalTo(0));
        post("/authz/role/create", request).body("code", equalTo(0));
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
