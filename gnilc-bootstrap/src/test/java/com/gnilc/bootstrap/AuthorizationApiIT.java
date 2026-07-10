package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.AppBaselineDataSeeder;
import com.gnilc.bootstrap.support.BootstrapTestConfiguration;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.equalTo;

@ApiTest
@Import(BootstrapTestConfiguration.class)
class AuthorizationApiIT extends ApiTestSupport {
    @LocalServerPort
    private int port;

    @Test
    void distinguishesAnonymousInvalidAndInsufficientCredentials() {
        requestSpecification(port)
                .when().post("/sys/admin/refresh")
                .then().statusCode(401)
                .body("code", equalTo(20002));

        requestSpecification(port)
                .when().get("/sys/admin/user-info")
                .then().statusCode(403)
                .body("code", equalTo(20003));

        requestSpecification(port)
                .header("Authorization", "Bearer sys_admin.999.invalid")
                .when().get("/sys/admin/user-info")
                .then().statusCode(401)
                .contentType("text/plain;charset=UTF-8")
                .body(equalTo("invalid access token"));

        String limitedToken = login(
                AppBaselineDataSeeder.LIMITED_USERNAME,
                AppBaselineDataSeeder.LIMITED_PASSWORD);
        requestSpecification(port)
                .header("Authorization", "Bearer " + limitedToken)
                .when().get("/sys/admin/user-info")
                .then().statusCode(403)
                .body("code", equalTo(20003));

        String adminToken = login(
                AppBaselineDataSeeder.ADMIN_USERNAME,
                AppBaselineDataSeeder.ADMIN_PASSWORD);
        requestSpecification(port)
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/sys/admin/user-info")
                .then().statusCode(200)
                .body("code", equalTo(0));
    }

    private String login(String username, String password) {
        return requestSpecification(port)
                .contentType("application/json")
                .body("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password))
                .when().post("/sys/admin/login")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .extract().path("data.accessToken");
    }
}
