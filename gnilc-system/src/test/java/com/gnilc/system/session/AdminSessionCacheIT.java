package com.gnilc.system.session;

import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.test.annotation.CleanTestData;
import com.gnilc.test.cleanup.CleanupMode;
import com.gnilc.test.cleanup.RedisTestCleanupConfiguration;
import com.gnilc.test.cleanup.TestDataResetListener;
import com.gnilc.test.container.RedisContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@CleanTestData(CleanupMode.REDIS_CLEAN)
@SpringBootTest(
        classes = {
                AdminSessionCacheIT.RedisTestConfiguration.class,
                AdminSessionRedisCommands.class,
                AdminSessionManager.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ContextConfiguration(initializers = RedisContainerContextInitializer.class)
@Import(RedisTestCleanupConfiguration.class)
@TestExecutionListeners(value = TestDataResetListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class AdminSessionCacheIT {
    private static final Duration ACCESS_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    @Autowired
    private AdminSessionManager sessionManager;
    @Autowired
    private AdminSessionRedisCommands redisCommands;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Test
    void createsBidirectionalPairsWithIndependentAccessAndRefreshTtls() {
        AdminSessionTokenPair pair = sessionManager.createSession(1001L);
        String accessKey = redisCommands.accessKey(1001L, pair.getAccessToken());
        String refreshKey = redisCommands.refreshKey(1001L, pair.getRefreshToken());

        assertThat(redisTemplate.opsForValue().get(accessKey)).isEqualTo(pair.getRefreshToken());
        assertThat(redisTemplate.opsForValue().get(refreshKey)).isEqualTo(pair.getAccessToken());
        assertTtlNear(accessKey, ACCESS_TTL);
        assertTtlNear(refreshKey, REFRESH_TTL);
        assertThat(sessionManager.validateAccessToken(pair.getAccessToken())).isEqualTo(1001L);
    }

    @Test
    void refreshRotatesTheAccessSideWhileKeepingTheRefreshTokenAndItsRemainingTtl() {
        AdminSessionTokenPair original = sessionManager.createSession(1001L);
        String refreshKey = redisCommands.refreshKey(1001L, original.getRefreshToken());
        long ttlBefore = redisTemplate.getExpire(refreshKey, java.util.concurrent.TimeUnit.SECONDS);

        AdminSessionTokenPair refreshed = sessionManager.refreshSession(original.getRefreshToken());

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.getRefreshToken()).isEqualTo(original.getRefreshToken());
        assertThat(refreshed.getAccessToken()).isNotEqualTo(original.getAccessToken());
        assertThat(redisTemplate.hasKey(redisCommands.accessKey(1001L, original.getAccessToken()))).isFalse();
        assertThat(redisTemplate.opsForValue().get(redisCommands.accessKey(1001L, refreshed.getAccessToken())))
                .isEqualTo(original.getRefreshToken());
        assertThat(redisTemplate.opsForValue().get(refreshKey)).isEqualTo(refreshed.getAccessToken());
        long ttlAfter = redisTemplate.getExpire(refreshKey, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttlAfter).isBetween(ttlBefore - 2, ttlBefore);
    }

    @Test
    void missingRefreshKeyIsNotCreatedByXxReplacement() {
        String refreshKey = redisCommands.refreshKey(1001L, "missing-refresh-token");

        boolean replaced = redisCommands.replacePairedAccessTokenKeepingTtl(
                1001L, "missing-refresh-token", "replacement-access-token");

        assertThat(replaced).isFalse();
        assertThat(redisTemplate.hasKey(refreshKey)).isFalse();
        assertThat(redisTemplate.opsForValue().get(refreshKey)).isNull();
    }

    @Test
    void logoutRevokesBothTokensInOnlyThatSession() {
        AdminSessionTokenPair first = sessionManager.createSession(1001L);
        AdminSessionTokenPair second = sessionManager.createSession(1001L);

        assertThat(sessionManager.logout(first.getRefreshToken())).isTrue();

        assertThat(sessionManager.validateAccessToken(first.getAccessToken())).isNull();
        assertThat(sessionManager.refreshSession(first.getRefreshToken())).isNull();
        assertThat(sessionManager.validateAccessToken(second.getAccessToken())).isEqualTo(1001L);
        assertThat(sessionManager.refreshSession(second.getRefreshToken())).isNotNull();
    }

    @Test
    void cleanupAndValidationKeepDifferentUsersIsolated() {
        AdminSessionTokenPair firstUser = sessionManager.createSession(1001L);
        AdminSessionTokenPair secondUser = sessionManager.createSession(2002L);

        sessionManager.cleanupUserSessions(1001L);

        assertThat(sessionManager.validateAccessToken(firstUser.getAccessToken())).isNull();
        assertThat(sessionManager.logout(firstUser.getRefreshToken())).isFalse();
        assertThat(sessionManager.validateAccessToken(secondUser.getAccessToken())).isEqualTo(2002L);
        assertThat(redisTemplate.hasKey(redisCommands.refreshKey(2002L, secondUser.getRefreshToken()))).isTrue();
        assertThat(redisTemplate.keys("sys:admin:*:1001:*")).isEmpty();
    }

    private void assertTtlNear(String key, Duration expected) {
        Long actualSeconds = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(actualSeconds).isNotNull();
        assertThat(actualSeconds).isPositive();
        assertThat(actualSeconds).isGreaterThan(expected.minusSeconds(10).getSeconds());
        assertThat(actualSeconds).isLessThanOrEqualTo(expected.getSeconds());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    static class RedisTestConfiguration {
    }
}
