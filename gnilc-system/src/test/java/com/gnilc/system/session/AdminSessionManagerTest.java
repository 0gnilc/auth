package com.gnilc.system.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionManagerTest {
    private final AdminSessionRedisCommands redis = mock(AdminSessionRedisCommands.class);
    private final AdminSessionTokenCodec codec = new AdminSessionTokenCodec();
    private final AdminSessionManager sessions = new AdminSessionManager(redis, codec);

    @Test
    void createAndValidateSessionUseAccessTokenMapping() {
        AdminSessionTokenPair pair = sessions.createSession(5L);
        when(redis.hasAccessToken(5L, pair.getAccessToken())).thenReturn(true);

        assertThat(sessions.validateAccessToken(pair.getAccessToken())).isEqualTo(5L);
        verify(redis).saveSession(5L, pair.getAccessToken(), pair.getRefreshToken());
    }

    @Test
    void invalidOrUnknownAccessTokenIsRejected() {
        assertThat(sessions.validateAccessToken("foreign")).isNull();
        String token = codec.issue(6L);
        when(redis.hasAccessToken(6L, token)).thenReturn(false);

        assertThat(sessions.validateAccessToken(token)).isNull();
    }

    @Test
    void refreshRotatesOnlyAccessTokenAndRevokesOldMapping() {
        String refresh = codec.issue(7L);
        String oldAccess = codec.issue(7L);
        when(redis.hasRefreshToken(7L, refresh)).thenReturn(true);
        when(redis.getPairedAccessToken(7L, refresh)).thenReturn(oldAccess);
        when(redis.replacePairedAccessTokenKeepingTtl(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(refresh), anyString())).thenReturn(true);

        AdminSessionTokenPair pair = sessions.refreshSession(refresh);

        assertThat(pair.getRefreshToken()).isEqualTo(refresh);
        assertThat(pair.getAccessToken()).isNotEqualTo(oldAccess);
        verify(redis).deleteAccessToken(7L, oldAccess);
        verify(redis).saveAccessToken(7L, pair.getAccessToken(), refresh);
    }

    @Test
    void failedRefreshDoesNotCreateANewAccessMapping() {
        String refresh = codec.issue(8L);
        when(redis.hasRefreshToken(8L, refresh)).thenReturn(true);
        when(redis.getPairedAccessToken(8L, refresh)).thenReturn("old");
        when(redis.replacePairedAccessTokenKeepingTtl(
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(refresh), anyString())).thenReturn(false);

        assertThat(sessions.refreshSession(refresh)).isNull();
        verify(redis, never()).saveAccessToken(
                org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    void logoutRevokesRefreshTokenAndItsPairedAccessToken() {
        String refresh = codec.issue(9L);
        String access = codec.issue(9L);
        when(redis.hasRefreshToken(9L, refresh)).thenReturn(true);
        when(redis.getPairedAccessToken(9L, refresh)).thenReturn(access);

        assertThat(sessions.logout(refresh)).isTrue();

        verify(redis).deleteAccessToken(9L, access);
        verify(redis).deleteRefreshToken(9L, refresh);
        sessions.cleanupUserSessions(9L);
        verify(redis).deleteUserSessions(9L);
    }
}
