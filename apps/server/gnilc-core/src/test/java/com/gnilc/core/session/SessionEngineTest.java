package com.gnilc.core.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 隔离 Redis 操作验证会话编排，刷新只轮换访问令牌，非法或跨身份域凭据不进入成功路径。 */
class SessionEngineTest {
    private final SessionPolicy policy = new SessionPolicy(
            "sys_admin", "sys_admin", Duration.ofDays(7), Duration.ofDays(30));
    private final SessionRedisStore store = mock(SessionRedisStore.class);
    private final SessionEngine sessions = new SessionEngine(policy, store);

    @Test
    void createAndValidateSessionUseTheConfiguredNamespace() {
        SessionTokenPair pair = sessions.createSession(5L);
        when(store.hasAccessToken(policy, 5L, pair.getAccessToken())).thenReturn(true);

        assertThat(sessions.validateAccessToken(pair.getAccessToken())).isEqualTo(5L);
        verify(store).saveSession(policy, 5L, pair.getAccessToken(), pair.getRefreshToken());
    }

    @Test
    void refreshRotatesOnlyAccessToken() {
        SessionTokenCodec codec = new SessionTokenCodec("sys_admin");
        String refresh = codec.issue(7L);
        String oldAccess = codec.issue(7L);
        when(store.hasRefreshToken(policy, 7L, refresh)).thenReturn(true);
        when(store.getPairedAccessToken(policy, 7L, refresh)).thenReturn(oldAccess);
        when(store.rotateAccessToken(
                org.mockito.ArgumentMatchers.eq(policy),
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(refresh),
                org.mockito.ArgumentMatchers.eq(oldAccess),
                anyString())).thenReturn(true);

        SessionTokenPair pair = sessions.refreshSession(refresh);

        assertThat(pair.getRefreshToken()).isEqualTo(refresh);
        assertThat(pair.getAccessToken()).isNotEqualTo(oldAccess);
        verify(store).rotateAccessToken(policy, 7L, refresh, oldAccess, pair.getAccessToken());
    }

    @Test
    void failedRefreshAndForeignTokensAreRejected() {
        assertThat(sessions.validateAccessToken("sys_admin.8.value")).isNull();
        assertThat(sessions.refreshSession("sys_admin.8.value")).isNull();

        SessionTokenCodec codec = new SessionTokenCodec("sys_admin");
        String refresh = codec.issue(8L);
        when(store.hasRefreshToken(policy, 8L, refresh)).thenReturn(true);
        when(store.getPairedAccessToken(policy, 8L, refresh)).thenReturn("old");
        assertThat(sessions.refreshSession(refresh)).isNull();
    }

    @Test
    void logoutAndCleanupUseTheConfiguredNamespace() {
        SessionTokenCodec codec = new SessionTokenCodec("sys_admin");
        String refresh = codec.issue(9L);
        when(store.deleteSession(policy, 9L, refresh)).thenReturn(true);

        assertThat(sessions.logout(refresh)).isTrue();

        verify(store).deleteSession(policy, 9L, refresh);
        sessions.cleanupUserSessions(9L);
        verify(store).deleteUserSessions(policy, 9L);
    }
}
