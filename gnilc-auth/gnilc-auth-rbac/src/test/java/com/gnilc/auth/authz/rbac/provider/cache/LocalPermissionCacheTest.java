package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalPermissionCacheTest {
    @Test
    void loadsOnceAndReloadsAfterReset() {
        PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
        Permission first = new Permission("first");
        Permission second = new Permission("second");
        when(loader.loadUserPermissions(9L)).thenReturn(List.of(first), List.of(second));
        LocalPermissionCache cache = new LocalPermissionCache(loader);

        assertThat(cache.loadUserPermissions(9L)).containsExactly(first);
        assertThat(cache.loadUserPermissions(9L)).containsExactly(first);
        cache.resetUserPermissions(9L);
        assertThat(cache.loadUserPermissions(9L)).containsExactly(second);
        assertThat(cache.loadUserPermissions(null)).isEmpty();
        verify(loader, times(2)).loadUserPermissions(9L);
        cache.shutdownResetExecutor();
    }

    @Test
    void normalizesNullLoaderResultsForAllReadModels() {
        PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
        when(loader.loadTargetPermissions()).thenReturn(null);
        when(loader.loadPublicAccessPermissions()).thenReturn(null);
        LocalPermissionCache cache = new LocalPermissionCache(loader);

        assertThat(cache.loadTargetPermissions()).isEmpty();
        assertThat(cache.loadPublicAccessPermissions()).isEmpty();
        cache.shutdownResetExecutor();
    }
}
