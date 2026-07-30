package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalPermissionCacheServiceTest {
    @Test
    void loadsOnceAndReloadsAfterReset() {
        PermissionCacheLoader cacheLoader = mock(PermissionCacheLoader.class);
        Permission first = new Permission("first");
        Permission second = new Permission("second");
        when(cacheLoader.loadUserPermissions(9L)).thenReturn(List.of(first), List.of(second));
        LocalPermissionCacheService cacheService = new LocalPermissionCacheService(cacheLoader);

        assertThat(cacheService.loadUserPermissions(9L)).containsExactly(first);
        assertThat(cacheService.loadUserPermissions(9L)).containsExactly(first);
        cacheService.resetUserPermissions(9L);
        assertThat(cacheService.loadUserPermissions(9L)).containsExactly(second);
        assertThat(cacheService.loadUserPermissions(null)).isEmpty();
        verify(cacheLoader, times(2)).loadUserPermissions(9L);
        cacheService.shutdownResetExecutor();
    }

    @Test
    void normalizesNullLoaderResultsForAllReadModels() {
        PermissionCacheLoader cacheLoader = mock(PermissionCacheLoader.class);
        when(cacheLoader.loadTargetPermissions()).thenReturn(null);
        when(cacheLoader.loadPublicAccessPermissions()).thenReturn(null);
        LocalPermissionCacheService cacheService = new LocalPermissionCacheService(cacheLoader);

        assertThat(cacheService.loadTargetPermissions()).isEmpty();
        assertThat(cacheService.loadPublicAccessPermissions()).isEmpty();
        cacheService.shutdownResetExecutor();
    }
}
