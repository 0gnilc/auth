package com.gnilc.system.i18n.api;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
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
class I18nApiIT extends AdminApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionCache permissionCache;

    @Test
    void defaultAdministratorCanManageAndReloadDynamicMessages() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.accessToken());

        given()
                .header("Authorization", auth)
                .header("X-Client", "admin")
                .post("/api/sys/i18n/bundle")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.zh-CN.menu.dashboard.title", equalTo("首页"));

        given()
                .header("Authorization", auth)
                .header("X-Client", "admin")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "i18nKey":"api.message.title",
                          "values":[
                            {"locale":"zh-CN","value":"接口消息"},
                            {"locale":"en-US","value":"API message"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.values", hasSize(2));

        given()
                .header("Authorization", auth)
                .header("X-Client", "admin")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "previousKey":"api.message.title",
                          "i18nKey":"api.message.heading",
                          "values":[{"locale":"zh-CN","value":"接口标题"}]
                        }
                        """)
                .post("/api/sys/i18n/save")
                .then()
                .statusCode(200)
                .body("data.i18nKey", equalTo("api.message.heading"))
                .body("data.values", hasSize(2));

        given()
                .header("Authorization", auth)
                .header("X-Client", "admin")
                .contentType(ContentType.JSON)
                .body("{\"i18nKey\":\"api.message.heading\"}")
                .post("/api/sys/i18n/remove")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE client = 'admin' AND i18n_key LIKE 'api.message.%'
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
        permissionCache.resetAll();

        TokenPair limited = loginAsLimitedAdmin();
        given()
                .header("Authorization", bearer(limited.accessToken()))
                .header("X-Client", "admin")
                .post("/api/sys/i18n/bundle")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        given()
                .header("Authorization", bearer(limited.accessToken()))
                .header("X-Client", "admin")
                .contentType(ContentType.JSON)
                .body("{\"currentPage\":1,\"pageSize\":10}")
                .post("/api/sys/i18n/page")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void i18nApisRequireAValidClientHeader() {
        TokenPair admin = loginAsDefaultAdmin();
        given()
                .header("Authorization", bearer(admin.accessToken()))
                .post("/api/sys/i18n/bundle")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001));
        given()
                .header("Authorization", bearer(admin.accessToken()))
                .header("Accept-Language", "en-US")
                .header("X-Client", "unknown")
                .post("/api/sys/i18n/bundle")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Client unknown is not supported."));
    }
}
