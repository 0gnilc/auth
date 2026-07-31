package com.gnilc.auth.authz.rbac.api;

import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.service.MenuService;
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

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@ApiTest
@Import(RestExceptionHandlingConfiguration.class)
@ContextConfiguration(
        classes = RbacTestApplication.class,
        initializers = RbacContainerContextInitializer.class)
class MenuApiIT extends ApiTestSupport {

    @Autowired
    private MenuService menus;

    @Test
    void menuHttpContractCreatesQueriesUpdatesRemovesAndRecreates() {
        post("/authz/menu/create", request("AuditCatalog", "/audit", "Audit"))
                .body("code", equalTo(0));
        MenuBo menu = menus.getMenuByPath("/audit");
        post("/authz/menu/tree", Map.of())
                .body("data.name", hasItem("AuditCatalog"));

        Map<String, Object> update = request(
                "AuditCatalogUpdated", "/audit-updated", "Audit Updated");
        update.put("id", menu.getId());
        post("/authz/menu/update", update).body("code", equalTo(0));
        post("/authz/menu/tree", Map.of())
                .body("data.name", hasItem("AuditCatalogUpdated"))
                .body("data.path", hasItem("/audit-updated"));

        post("/authz/menu/remove/" + menu.getId(), Map.of()).body("code", equalTo(0));
        post("/authz/menu/create", request(
                "AuditCatalogUpdated", "/audit-updated", "Audit Recreated"))
                .body("code", equalTo(0));
        MenuBo recreated = menus.getMenuByPath("/audit-updated");
        assertThat(recreated).isNotNull();
        post("/authz/menu/remove/" + recreated.getId(), Map.of()).body("code", equalTo(0));
        assertThat(menus.getMenuByPath("/audit-updated")).isNull();
    }

    @Test
    void menuBoundariesRejectOverflowAndKeepMaximumUnicodeValuesReusable() {
        post("/authz/menu/create", request(
                "m".repeat(256), "/oversized", "Oversized menu"))
                .body("code", equalTo(10001));
        assertThat(menus.getMenuByPath("/oversized")).isNull();

        Map<String, Object> request = request(
                "\uD83D\uDE00".repeat(255),
                "/" + "x".repeat(499),
                "\uD83D\uDE00".repeat(255));
        post("/authz/menu/create", request).body("code", equalTo(0));
        MenuBo menu = menus.getMenuByPath("/" + "x".repeat(499));
        post("/authz/menu/remove/" + menu.getId(), Map.of()).body("code", equalTo(0));
        post("/authz/menu/create", request).body("code", equalTo(0));
    }

    private Map<String, Object> request(String name, String path, String title) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("pid", 0);
        request.put("type", "CATALOG");
        request.put("status", true);
        request.put("name", name);
        request.put("path", path);
        request.put("order", 1);
        request.put("title", title);
        return request;
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
