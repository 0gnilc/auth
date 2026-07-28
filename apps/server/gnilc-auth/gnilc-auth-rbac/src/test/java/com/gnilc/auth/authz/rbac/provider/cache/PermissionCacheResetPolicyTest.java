package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionCacheResetPolicyTest {
    @Test
    void mapsPermissionAndRoleChangesToAffectedCaches() {
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        UserRoleService userRoles = mock(UserRoleService.class);
        when(rolePermissions.getRoleIds(11L)).thenReturn(List.of(2L, 3L));
        when(userRoles.getUserIds(List.of(2L, 3L))).thenReturn(List.of(7L, 7L, 8L));
        when(userRoles.getUserIds(2L)).thenReturn(List.of(7L));
        PermissionCacheResetPolicy policy = new PermissionCacheResetPolicy(rolePermissions, userRoles);

        assertThat(policy.commandsFor(AuthorizationEvent.of(
                AuthorizationEvent.Type.PERMISSION, AuthorizationEvent.Action.UPDATE, 11L)))
                .containsExactly(PermissionCacheResetCommand.targetPermissions(),
                        PermissionCacheResetCommand.publicAccessPermissions(),
                        PermissionCacheResetCommand.userPermissions(7L),
                        PermissionCacheResetCommand.userPermissions(8L));
        assertThat(policy.commandsFor(AuthorizationEvent.of(
                AuthorizationEvent.Type.ROLE, AuthorizationEvent.Action.UPDATE, 2L)))
                .containsExactly(PermissionCacheResetCommand.userPermissions(7L));
        assertThat(policy.commandsFor(AuthorizationEvent.of(
                AuthorizationEvent.Type.ROLE_MENU, AuthorizationEvent.Action.REPLACE, 2L)))
                .isEmpty();
        assertThat(policy.commandsForAll(AuthorizationEvent.of(
                AuthorizationEvent.Type.ALL, AuthorizationEvent.Action.CLEAR)))
                .containsExactly(PermissionCacheResetCommand.all());
    }
}
