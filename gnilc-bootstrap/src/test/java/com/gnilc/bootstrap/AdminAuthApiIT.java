package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.AppBaselineDataSeeder;
import com.gnilc.bootstrap.support.BootstrapTestConfiguration;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ApiTest
@Import(BootstrapTestConfiguration.class)
class AdminAuthApiIT extends ApiTestSupport {
    private static final Duration ACCESS_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void loginRefreshAndLogoutManageOneSessionLifecycle() {
        var loginResponse = requestSpecification(port)
                .contentType("application/json")
                .body("""
                        {"username":"%s","password":"%s"}
                        """.formatted(AppBaselineDataSeeder.ADMIN_USERNAME, AppBaselineDataSeeder.ADMIN_PASSWORD))
                .when().post("/sys/admin/login")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue())
                .extract().response();
        String refreshToken = loginResponse.path("data.refreshToken");
        String oldAccessToken = loginResponse.path("data.accessToken");
        long userId = tokenUserId(oldAccessToken);
        String oldAccessKey = accessKey(userId, oldAccessToken);
        String refreshKey = refreshKey(userId, refreshToken);
        assertLiveKey(oldAccessKey, ACCESS_TTL);
        assertLiveKey(refreshKey, REFRESH_TTL);
        long refreshTtlBefore = redisTemplate.getExpire(refreshKey);

        var refreshResponse = requestSpecification(port)
                .header("X-Refresh-Token", refreshToken)
                .when().post("/sys/admin/refresh")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.refreshToken", equalTo(refreshToken))
                .extract().response();
        String newAccessToken = refreshResponse.path("data.accessToken");
        assertThat(newAccessToken).isNotBlank().isNotEqualTo(oldAccessToken);
        assertThat(redisTemplate.hasKey(oldAccessKey)).isFalse();
        assertLiveKey(accessKey(userId, newAccessToken), ACCESS_TTL);
        assertLiveKey(refreshKey, REFRESH_TTL);
        assertThat(redisTemplate.getExpire(refreshKey)).isLessThanOrEqualTo(refreshTtlBefore);
        assertThat(redisTemplate.opsForValue().get(refreshKey)).isEqualTo(newAccessToken);

        requestSpecification(port).header("Authorization", "Bearer " + oldAccessToken)
                .when().get("/sys/admin/user-info")
                .then().statusCode(401)
                .contentType("text/plain;charset=UTF-8")
                .body(equalTo("invalid access token"));

        requestSpecification(port).header("X-Refresh-Token", refreshToken)
                .when().post("/sys/admin/logout")
                .then().statusCode(200)
                .body("code", equalTo(0));
        assertThat(redisTemplate.hasKey(accessKey(userId, newAccessToken))).isFalse();
        assertThat(redisTemplate.hasKey(refreshKey)).isFalse();

        requestSpecification(port).header("X-Refresh-Token", refreshToken)
                .when().post("/sys/admin/refresh")
                .then().statusCode(401)
                .body("code", equalTo(20002));
        requestSpecification(port).header("Authorization", "Bearer " + newAccessToken)
                .when().get("/sys/admin/user-info")
                .then().statusCode(401)
                .contentType("text/plain;charset=UTF-8")
                .body(equalTo("invalid access token"));
    }

    private void assertLiveKey(String key, Duration maximumTtl) {
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.getExpire(key)).isPositive().isLessThanOrEqualTo(maximumTtl.getSeconds());
    }

    private long tokenUserId(String token) {
        return Long.parseLong(token.split("\\.", 3)[1]);
    }

    private String accessKey(long userId, String token) {
        return "sys:admin:at:" + userId + ":" + token;
    }

    private String refreshKey(long userId, String token) {
        return "sys:admin:rt:" + userId + ":" + token;
    }
}
