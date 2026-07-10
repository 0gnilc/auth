package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.AppBaselineDataSeeder;
import com.gnilc.bootstrap.support.BootstrapTestConfiguration;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ApiTest
@Import(BootstrapTestConfiguration.class)
class AdminManagementApiIT extends ApiTestSupport {
    @LocalServerPort
    private int port;

    @Test
    void administratorCanCreateReplaceRolesDisableAndObserveLoginFailure() {
        String token = login();

        requestSpecification(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "username":"created-admin",
                          "password":"CreatedAdmin1!",
                          "nickname":"Created Administrator",
                          "status":true,
                          "roleCodes":["test-limited"]
                        }
                        """)
                .when().post("/sys/admin/create")
                .then().statusCode(200)
                .body("code", equalTo(0));

        Response created = query(token, "created-admin");
        List<String> ids = created.path("data.list.id");
        assertThat(ids).hasSize(1);
        long adminId = Long.parseLong(ids.get(0));
        assertThat(created.<List<String>>path("data.list[0].roleCodes"))
                .containsExactly("test-limited");
        assertThat(created.<Boolean>path("data.list[0].status")).isTrue();

        requestSpecification(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"id":%d,"roleCodes":["test-admin"]}
                        """.formatted(adminId))
                .when().post("/sys/admin/update-roles")
                .then().statusCode(200)
                .body("code", equalTo(0));

        Response roleUpdated = query(token, "created-admin");
        assertThat(roleUpdated.<List<String>>path("data.list[0].roleCodes"))
                .containsExactly("test-admin")
                .doesNotContain("test-limited");

        requestSpecification(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"id":%d,"status":false}
                        """.formatted(adminId))
                .when().post("/sys/admin/update")
                .then().statusCode(200)
                .body("code", equalTo(0));

        Response disabled = query(token, "created-admin");
        assertThat(disabled.<Boolean>path("data.list[0].status")).isFalse();
        assertThat(disabled.<List<String>>path("data.list[0].roleCodes"))
                .containsExactly("test-admin");

        requestSpecification(port)
                .contentType("application/json")
                .body("""
                        {"username":"created-admin","password":"CreatedAdmin1!"}
                        """)
                .when().post("/sys/admin/login")
                .then().statusCode(200)
                .body("code", equalTo(20001))
                .body("error", equalTo("用户名或密码错误"));
    }

    private Response query(String token, String username) {
        Response response = requestSpecification(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"username":"%s"}
                        """.formatted(username))
                .when().post("/sys/admin/page")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .extract().response();
        assertThat(response.<String>path("data.totalCount")).isEqualTo("1");
        return response;
    }

    private String login() {
        return requestSpecification(port)
                .contentType("application/json")
                .body("""
                        {"username":"%s","password":"%s"}
                        """.formatted(AppBaselineDataSeeder.ADMIN_USERNAME, AppBaselineDataSeeder.ADMIN_PASSWORD))
                .when().post("/sys/admin/login")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .extract().path("data.accessToken");
    }
}
