package com.gnilc.system.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionManagerTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AdminSessionManager manager;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        manager = new AdminSessionManager(new AdminSessionRedisCommands(redisTemplate));
    }

    // TestCaseId: SYS-SESSION-001
    @Test
    void createsAccessAndRefreshSessionsWithPairedTokenValues() {
        AdminSessionTokenPair token = manager.createSession(1001L);

        assertThat(token.getAccessToken()).startsWith("sys_admin.1001.");
        assertThat(token.getRefreshToken()).startsWith("sys_admin.1001.");
        assertThat(token.getAccessToken()).isNotEqualTo(token.getRefreshToken());
        verify(valueOperations).set("sys:admin:at:1001:" + token.getAccessToken(), token.getRefreshToken(), Duration.ofDays(7));
        verify(valueOperations).set("sys:admin:rt:1001:" + token.getRefreshToken(), token.getAccessToken(), Duration.ofDays(30));
    }

    // TestCaseId: SYS-SESSION-002
    @Test
    void validatesAccessTokenByAccessKeyExistenceOnly() {
        when(redisTemplate.hasKey("sys:admin:at:1001:sys_admin.1001.access-token")).thenReturn(true);

        assertThat(manager.validateAccessToken("sys_admin.1001.access-token")).isEqualTo(1001L);
    }

    // TestCaseId: SYS-SESSION-003
    @Test
    void rejectsMissingAccessKey() {
        when(redisTemplate.hasKey("sys:admin:at:1001:sys_admin.1001.access-token")).thenReturn(false);

        assertThat(manager.validateAccessToken("sys_admin.1001.access-token")).isNull();
    }

    // TestCaseId: SYS-SESSION-004
    @Test
    void rejectsMalformedTokensWithoutTouchingRedisSessionKeys() {
        assertThat(manager.validateAccessToken("not-a-token")).isNull();
        assertThat(manager.refreshSession("not-a-token")).isNull();
        assertThat(manager.logout("not-a-token")).isFalse();

        verify(redisTemplate, never()).hasKey(any());
        verify(valueOperations, never()).get(any());
    }

    // TestCaseId: SYS-SESSION-005
    @Test
    void refreshRequiresRefreshKeyAndKeepsRefreshTokenAndTtl() {
        when(redisTemplate.hasKey("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(true);
        when(valueOperations.get("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn("sys_admin.1001.old-access-token");
        when(redisTemplate.execute(ArgumentMatchers.<RedisCallback<Boolean>>any())).thenReturn(true);

        AdminSessionTokenPair token = manager.refreshSession("sys_admin.1001.refresh-token");

        assertThat(token.getAccessToken()).startsWith("sys_admin.1001.");
        assertThat(token.getAccessToken()).isNotEqualTo("sys_admin.1001.old-access-token");
        assertThat(token.getRefreshToken()).isEqualTo("sys_admin.1001.refresh-token");
        verify(redisTemplate).delete("sys:admin:at:1001:sys_admin.1001.old-access-token");
        verify(redisTemplate, never()).delete("sys:admin:rt:1001:sys_admin.1001.refresh-token");
        verify(valueOperations).set("sys:admin:at:1001:" + token.getAccessToken(), "sys_admin.1001.refresh-token", Duration.ofDays(7));
    }

    // TestCaseId: SYS-SESSION-006
    @Test
    void logoutRequiresRefreshKeyAndDeletesPairedTokens() {
        when(redisTemplate.hasKey("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(true);
        when(valueOperations.get("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn("sys_admin.1001.access-token");

        assertThat(manager.logout("sys_admin.1001.refresh-token")).isTrue();

        verify(redisTemplate).delete("sys:admin:at:1001:sys_admin.1001.access-token");
        verify(redisTemplate).delete("sys:admin:rt:1001:sys_admin.1001.refresh-token");
    }

    // TestCaseId: SYS-SESSION-007
    @Test
    void cleanupDeletesAllUserAccessAndRefreshKeys() {
        when(redisTemplate.keys("sys:admin:at:1001:*")).thenReturn(Set.of("at-1", "at-2"));
        when(redisTemplate.keys("sys:admin:rt:1001:*")).thenReturn(Set.of("rt-1"));

        manager.cleanupUserSessions(1001L);

        verify(redisTemplate).delete(Set.of("at-1", "at-2"));
        verify(redisTemplate).delete(Set.of("rt-1"));
    }

    // TestCaseId: SYS-SESSION-008
    @Test
    void supportsOnlyAdminNamespacedAccessTokens() {
        assertThat(manager.supportsAccessToken("sys_admin.1001.access-token")).isTrue();
        assertThat(manager.supportsAccessToken("1001.access-token")).isFalse();
        assertThat(manager.supportsAccessToken(null)).isFalse();
    }

    // TestCaseId: SYS-SESSION-009
    @Test
    void refreshReturnsNullWhenPairedAccessTokenIsBlankOrReplaceFails() {
        when(redisTemplate.hasKey("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(true);
        when(valueOperations.get("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(" ");

        assertThat(manager.refreshSession("sys_admin.1001.refresh-token")).isNull();
        verify(redisTemplate, never()).execute(ArgumentMatchers.<RedisCallback<Boolean>>any());

        when(valueOperations.get("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn("sys_admin.1001.old-access-token");
        when(redisTemplate.execute(ArgumentMatchers.<RedisCallback<Boolean>>any())).thenReturn(false);

        assertThat(manager.refreshSession("sys_admin.1001.refresh-token")).isNull();
        verify(redisTemplate, never()).delete("sys:admin:at:1001:sys_admin.1001.old-access-token");
    }

    // TestCaseId: SYS-SESSION-010
    @Test
    void logoutWithBlankPairedAccessTokenDeletesOnlyRefreshToken() {
        when(redisTemplate.hasKey("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(true);
        when(valueOperations.get("sys:admin:rt:1001:sys_admin.1001.refresh-token")).thenReturn(" ");

        assertThat(manager.logout("sys_admin.1001.refresh-token")).isTrue();

        verify(redisTemplate, never()).delete("sys:admin:at:1001: ");
        verify(redisTemplate).delete("sys:admin:rt:1001:sys_admin.1001.refresh-token");
    }

}
