package com.gnilc.system.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionManagerTest {
    private AdminSessionRedisCommands redisCommands;
    private AdminSessionTokenCodec tokenCodec;
    private AdminSessionManager manager;

    @BeforeEach
    void setUp() {
        redisCommands = mock(AdminSessionRedisCommands.class);
        tokenCodec = mock(AdminSessionTokenCodec.class);
        manager = new AdminSessionManager(redisCommands, tokenCodec);
    }

    @Test
    void createsAndPersistsOnePairedSession() {
        when(tokenCodec.issue(1001L)).thenReturn("access-token", "refresh-token");

        AdminSessionTokenPair pair = manager.createSession(1001L);

        assertThat(pair.getAccessToken()).isEqualTo("access-token");
        assertThat(pair.getRefreshToken()).isEqualTo("refresh-token");
        verify(redisCommands).saveSession(1001L, "access-token", "refresh-token");
    }

    @Test
    void validatesOnlyExistingAccessTokens() {
        when(tokenCodec.resolve("access-token")).thenReturn(1001L);
        when(redisCommands.hasAccessToken(1001L, "access-token")).thenReturn(true);

        assertThat(manager.validateAccessToken("access-token")).isEqualTo(1001L);

        when(redisCommands.hasAccessToken(1001L, "access-token")).thenReturn(false);
        assertThat(manager.validateAccessToken("access-token")).isNull();
    }

    @Test
    void malformedTokensAreRejectedWithoutReadingSessionStorage() {
        when(tokenCodec.resolve("malformed")).thenThrow(new IllegalArgumentException("bad token"));

        assertThat(manager.validateAccessToken("malformed")).isNull();
        assertThat(manager.refreshSession("malformed")).isNull();
        assertThat(manager.logout("malformed")).isFalse();

        verify(redisCommands, never()).hasAccessToken(org.mockito.ArgumentMatchers.any(), anyString());
        verify(redisCommands, never()).hasRefreshToken(org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void refreshReplacesOnlyTheAccessTokenAndKeepsTheRefreshToken() {
        when(tokenCodec.resolve("refresh-token")).thenReturn(1001L);
        when(redisCommands.hasRefreshToken(1001L, "refresh-token")).thenReturn(true);
        when(redisCommands.getPairedAccessToken(1001L, "refresh-token")).thenReturn("old-access-token");
        when(tokenCodec.issue(1001L)).thenReturn("new-access-token");
        when(redisCommands.replacePairedAccessTokenKeepingTtl(1001L, "refresh-token", "new-access-token"))
                .thenReturn(true);

        AdminSessionTokenPair pair = manager.refreshSession("refresh-token");

        assertThat(pair.getAccessToken()).isEqualTo("new-access-token");
        assertThat(pair.getRefreshToken()).isEqualTo("refresh-token");
        verify(redisCommands).deleteAccessToken(1001L, "old-access-token");
        verify(redisCommands).saveAccessToken(1001L, "new-access-token", "refresh-token");
        verify(redisCommands, never()).deleteRefreshToken(1001L, "refresh-token");
    }

    @Test
    void failedAtomicRefreshLeavesTheOldPairUntouched() {
        when(tokenCodec.resolve("refresh-token")).thenReturn(1001L);
        when(redisCommands.hasRefreshToken(1001L, "refresh-token")).thenReturn(true);
        when(redisCommands.getPairedAccessToken(1001L, "refresh-token")).thenReturn("old-access-token");
        when(tokenCodec.issue(1001L)).thenReturn("new-access-token");
        when(redisCommands.replacePairedAccessTokenKeepingTtl(1001L, "refresh-token", "new-access-token"))
                .thenReturn(false);

        assertThat(manager.refreshSession("refresh-token")).isNull();

        verify(redisCommands, never()).deleteAccessToken(1001L, "old-access-token");
        verify(redisCommands, never()).saveAccessToken(1001L, "new-access-token", "refresh-token");
    }

    @Test
    void logoutRevokesBothSidesOfThePair() {
        when(tokenCodec.resolve("refresh-token")).thenReturn(1001L);
        when(redisCommands.hasRefreshToken(1001L, "refresh-token")).thenReturn(true);
        when(redisCommands.getPairedAccessToken(1001L, "refresh-token")).thenReturn("access-token");

        assertThat(manager.logout("refresh-token")).isTrue();

        verify(redisCommands).deleteAccessToken(1001L, "access-token");
        verify(redisCommands).deleteRefreshToken(1001L, "refresh-token");
    }

    @Test
    void cleanupRevokesEverySessionForOnlyTheRequestedUser() {
        manager.cleanupUserSessions(1001L);

        verify(redisCommands).deleteUserSessions(1001L);
    }
}
