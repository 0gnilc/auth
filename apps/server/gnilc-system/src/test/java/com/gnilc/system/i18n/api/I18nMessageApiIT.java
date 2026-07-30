package com.gnilc.system.i18n.api;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.system.admin.support.AdminApiTestConfiguration;
import com.gnilc.system.admin.support.AdminApiTestSupport;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@ApiTest
@Import(AdminApiTestConfiguration.class)
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class I18nMessageApiIT extends AdminApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionCacheService cacheService;

    @Test
    void defaultAdministratorCanManageAndReloadDynamicMessages() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.accessToken());

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/bundle/admin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.zh-CN.menu.dashboard.title", equalTo("首页"));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/categories")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2))
                .body("data[0]", equalTo("default"))
                .body("data[1]", equalTo("admin"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"default",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"zh-CN","value":"接口消息"},
                            {"locale":"en-US","value":"API message"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.category", equalTo("default"))
                .body("data.values", hasSize(2));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"admin",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"en-US","value":"Replacement"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo(
                        "The target internationalization key api.message.title already exists."));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/api.message.title")
                .then()
                .statusCode(200)
                .body("data.category", equalTo("default"))
                .body("data.values[1].value", equalTo("API message"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"admin",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"zh-CN","value":""},
                            {"locale":"en-US","value":"API heading"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/save")
                .then()
                .statusCode(200)
                .body("data.category", equalTo("admin"))
                .body("data.messageKey", equalTo("api.message.title"))
                .body("data.values", hasSize(1));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/api.message.title")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.category", equalTo("admin"))
                .body("data.messageKey", equalTo("api.message.title"))
                .body("data.values[0].locale", equalTo("en-US"));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/remove/api.message.title")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE category = 'admin' AND message_key LIKE 'api.message.%'
                """, Integer.class)).isZero();
    }

    @Test
    void baselineAdministratorCanReadBundleButCannotManageMessages() {
        Long limitedUserId = jdbc.queryForObject("""
                SELECT user_id FROM sys_admin WHERE username = 'limited' AND del = 0
                """, Long.class);
        Long adminRoleId = jdbc.queryForObject("""
                SELECT id FROM az_role WHERE code = 'admin' AND del = 0
                """, Long.class);
        jdbc.update("""
                INSERT INTO az_user_role (del, create_time, user_id, role_id)
                VALUES (0, NOW(), ?, ?)
                """, limitedUserId, adminRoleId);
        cacheService.resetAll();

        TokenPair limited = loginAsLimitedAdmin();
        given()
                .header("Authorization", bearer(limited.accessToken()))
                .post("/api/sys/i18n-message/bundle/admin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        given()
                .header("Authorization", bearer(limited.accessToken()))
                .contentType(ContentType.JSON)
                .body("{\"currentPage\":1,\"pageSize\":10}")
                .post("/api/sys/i18n-message/page")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void runtimeBundleRequiresASupportedCategoryPath() {
        TokenPair admin = loginAsDefaultAdmin();
        given()
                .header("Authorization", bearer(admin.accessToken()))
                .header("Accept-Language", "en-US")
                .post("/api/sys/i18n-message/bundle/unknown")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Category unknown is not supported."));
    }

    @Test
    void pathKeysUseTheExistingServiceValidationAndErrorEnvelope() {
        String auth = bearer(loginAsDefaultAdmin().accessToken());

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/menu..title")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("The internationalization key must be a valid dot path."));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/remove/{messageKey}", "a".repeat(192))
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("The internationalization key must not exceed 191 characters."));
    }
}
