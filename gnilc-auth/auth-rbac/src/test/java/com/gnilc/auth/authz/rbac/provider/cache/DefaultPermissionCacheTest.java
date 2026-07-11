package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultPermissionCacheTest {
    private final PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
    private final DefaultPermissionCache cache = new DefaultPermissionCache(loader);

    @Test
    void delegatesEveryPermissionViewWithoutRetainingLocalState() {
        List<TargetPermission> firstTargets = List.of(new TargetPermission("/accounts/**", "account:read"));
        List<TargetPermission> secondTargets = List.of(new TargetPermission("/roles/**", "role:read"));
        List<Permission> firstUserPermissions = List.of(new Permission("account:write"));
        List<Permission> secondUserPermissions = List.of(new Permission("role:write"));
        List<Permission> firstPublicPermissions = List.of(new Permission("account:public"));
        List<Permission> secondPublicPermissions = List.of(new Permission("role:public"));
        when(loader.loadTargetPermissions()).thenReturn(firstTargets, secondTargets);
        when(loader.loadUserPermissions(42L)).thenReturn(firstUserPermissions, secondUserPermissions);
        when(loader.loadPublicAccessPermissions()).thenReturn(firstPublicPermissions, secondPublicPermissions);

        assertThat(cache.loadTargetPermissions()).isSameAs(firstTargets);
        assertThat(cache.loadTargetPermissions()).isSameAs(secondTargets);
        assertThat(cache.loadUserPermissions(42L)).isSameAs(firstUserPermissions);
        assertThat(cache.loadUserPermissions(42L)).isSameAs(secondUserPermissions);
        assertThat(cache.loadPublicAccessPermissions()).isSameAs(firstPublicPermissions);
        assertThat(cache.loadPublicAccessPermissions()).isSameAs(secondPublicPermissions);

        verify(loader, times(2)).loadTargetPermissions();
        verify(loader, times(2)).loadUserPermissions(42L);
        verify(loader, times(2)).loadPublicAccessPermissions();
    }

    @Test
    void resetOperationsAreNoOpsBecauseTheImplementationHasNoLocalState() {
        cache.resetTargetPermissions();
        cache.resetUserPermissions(42L);
        cache.resetPublicAccessPermissions();
        cache.resetAll();

        verifyNoInteractions(loader);
    }
}
