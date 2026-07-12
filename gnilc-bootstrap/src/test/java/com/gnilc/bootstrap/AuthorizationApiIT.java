package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.AdminApiTestSupport;
import com.gnilc.bootstrap.support.BootstrapContainerContextInitializer;
import com.gnilc.bootstrap.support.BootstrapTestConfiguration;
import com.gnilc.test.annotation.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@ApiTest
@Import(BootstrapTestConfiguration.class)
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
class AuthorizationApiIT extends AdminApiTestSupport {
    @Test
    void anonymousProtectedRequestIsForbiddenWithJsonContract() {
        given()
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(403)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20003))
                .body("error", equalTo("access denied"));
    }

    @Test
    void authenticatedAdminReceivesRolesAndButtonAccessCodes() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/role-codes")
                .then()
                .statusCode(200)
                .body("data", hasItem("admin"));
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/menu/access-codes")
                .then()
                .statusCode(200)
                .body("data", hasItem("admin:manage"));
    }

    @Test
    void authenticatedUserWithoutRequiredPermissionIsForbidden() {
        TokenPair pair = loginAsLimitedAdmin();

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void revokedNamespacedAccessTokenReturns401BeforeAuthorization() {
        TokenPair pair = loginAsDefaultAdmin();
        given().header("X-Refresh-Token", pair.refreshToken())
                .post("/api/sys/admin/logout")
                .then().statusCode(200);

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401);
    }
}
