package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionCacheTest {
    @Test
    void localCacheLoadsOnceAndReloadsAfterReset() {
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
    void localCacheNormalizesNullLoaderResultsForAllReadModels() {
        PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
        when(loader.loadTargetPermissions()).thenReturn(null);
        when(loader.loadPublicAccessPermissions()).thenReturn(null);
        LocalPermissionCache cache = new LocalPermissionCache(loader);

        assertThat(cache.loadTargetPermissions()).isEmpty();
        assertThat(cache.loadPublicAccessPermissions()).isEmpty();

        cache.shutdownResetExecutor();
    }

    @Test
    void resetPolicyMapsPermissionAndRoleChangesToAffectedCaches() {
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        UserRoleService userRoles = mock(UserRoleService.class);
        when(rolePermissions.getRoleIds(11L)).thenReturn(List.of(2L, 3L));
        when(userRoles.getUserIds(List.of(2L, 3L))).thenReturn(List.of(7L, 7L, 8L));
        when(userRoles.getUserIds(2L)).thenReturn(List.of(7L));
        PermissionCacheResetPolicy policy = new PermissionCacheResetPolicy(rolePermissions, userRoles);

        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION, RbacAuthzEvent.Action.UPDATE, 11L)))
                .containsExactly(
                        PermissionCacheResetCommand.targetPermissions(),
                        PermissionCacheResetCommand.publicAccessPermissions(),
                        PermissionCacheResetCommand.userPermissions(7L),
                        PermissionCacheResetCommand.userPermissions(8L));
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE, RbacAuthzEvent.Action.UPDATE, 2L)))
                .containsExactly(PermissionCacheResetCommand.userPermissions(7L));
        assertThat(policy.commandsForAll(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL, RbacAuthzEvent.Action.CLEAR)))
                .containsExactly(PermissionCacheResetCommand.all());
    }

    @Test
    void resetExecutorRoutesEveryCommandType() {
        PermissionCache cache = mock(PermissionCache.class);
        PermissionCacheResetExecutor executor = new PermissionCacheResetExecutor(cache);

        executor.execute(PermissionCacheResetCommand.targetPermissions());
        executor.execute(PermissionCacheResetCommand.publicAccessPermissions());
        executor.execute(PermissionCacheResetCommand.userPermissions(5L));
        executor.execute(PermissionCacheResetCommand.all());
        executor.execute(null);

        verify(cache).resetTargetPermissions();
        verify(cache).resetPublicAccessPermissions();
        verify(cache).resetUserPermissions(5L);
        verify(cache).resetAll();
    }

    @Test
    void defaultCacheIsAStatelessLoaderAdapter() {
        PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
        TargetPermission target = new TargetPermission("/x", "read");
        when(loader.loadTargetPermissions()).thenReturn(List.of(target));

        assertThat(new DefaultPermissionCache(loader).loadTargetPermissions()).containsExactly(target);
    }
}
