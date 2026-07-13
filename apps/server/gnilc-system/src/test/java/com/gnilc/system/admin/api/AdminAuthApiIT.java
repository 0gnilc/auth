package com.gnilc.system.admin.api;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.system.admin.support.AdminApiTestConfiguration;
import com.gnilc.system.admin.support.AdminApiTestSupport;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@ApiTest
@Import({
        AdminApiTestConfiguration.class,
        RestExceptionHandlingConfiguration.class
})
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class AdminAuthApiIT extends AdminApiTestSupport {
    @Test
    void loginRefreshAndLogoutRunThroughTheRealHttpAndRedisStack() {
        TokenPair pair = loginAsDefaultAdmin();

        String accessToken = given()
                .header("X-Refresh-Token", pair.refreshToken())
                .when()
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.refreshToken", equalTo(pair.refreshToken()))
                .body("data.accessToken", not(equalTo(pair.accessToken())))
                .extract()
                .path("data.accessToken");

        given()
                .header("Authorization", bearer(accessToken))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("data.username", equalTo("admin"));

        given()
                .header("X-Refresh-Token", pair.refreshToken())
                .when()
                .post("/api/sys/admin/logout")
                .then()
                .statusCode(200);

        given()
                .header("X-Refresh-Token", pair.refreshToken())
                .when()
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(401)
                .body("code", equalTo(20002));
    }

    @Test
    void invalidCredentialsReturnAuthenticationBusinessError() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"admin","password":"wrong"}
                        """)
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(20001))
                .body("error", equalTo("Incorrect username or password."));
    }

    @Test
    void malformedLoginRequestUsesTheCommonExceptionFormat() {
        given()
                .contentType(ContentType.JSON)
                .body("{")
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(400)
                .body("code", equalTo(10001))
                .body("error", equalTo("The request body is malformed."));
    }
}
