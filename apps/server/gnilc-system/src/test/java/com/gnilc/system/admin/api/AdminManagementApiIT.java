package com.gnilc.system.admin.api;

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
import static org.hamcrest.Matchers.hasItem;

@ApiTest
@Import(AdminApiTestConfiguration.class)
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class AdminManagementApiIT extends AdminApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createQueryUpdateRolesAndRemoveAdminThroughApi() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.accessToken());

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username":"api-user",
                          "password":"Strong#123",
                          "nickname":"API User",
                          "homePath":"/workspace",
                          "status":true,
                          "roleCodes":["admin"]
                        }
                        """)
                .when()
                .post("/api/sys/admin/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = 'api-user'", Long.class);
        Long userId = jdbc.queryForObject(
                "select user_id from sys_admin where id = ?", Long.class, adminId);
        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"roleCodes\":[]}")
                .when()
                .post("/api/sys/admin/update-roles")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(activeRoleBindingCount(userId)).isZero();

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"roleCodes\":[\"admin\"]}")
                .when()
                .post("/api/sys/admin/update-roles")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(activeRoleBindingCount(userId)).isEqualTo(1);

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"api-user","currentPage":1,"pageSize":10}
                        """)
                .when()
                .post("/api/sys/admin/page")
                .then()
                .statusCode(200)
                .body("data.totalCount", equalTo("1"))
                .body("data.list.username", hasItem("api-user"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"nickname\":\"Updated User\"}")
                .when()
                .post("/api/sys/admin/update")
                .then()
                .statusCode(200);
        assertThat(jdbc.queryForObject(
                "select nickname from sys_admin where id = ?", String.class, adminId))
                .isEqualTo("Updated User");

        given()
                .header("Authorization", auth)
                .when()
                .post("/api/sys/admin/remove/{id}", adminId)
                .then()
                .statusCode(200);
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_admin where id = ? and del = 0", Integer.class, adminId))
                .isZero();
    }

    private int activeRoleBindingCount(Long userId) {
        return jdbc.queryForObject(
                "select count(*) from az_user_role where user_id = ? and del = 0",
                Integer.class,
                userId);
    }
}
