package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionCacheResetPolicyTest {
    private RolePermissionService rolePermissionService;
    private UserRoleService userRoleService;
    private PermissionCacheResetPolicy policy;

    @BeforeEach
    void setUp() {
        rolePermissionService = mock(RolePermissionService.class);
        userRoleService = mock(UserRoleService.class);
        policy = new PermissionCacheResetPolicy(rolePermissionService, userRoleService);
    }

    @Test
    void permissionChangesResetGlobalViewsAndEveryAffectedUserOnce() {
        when(rolePermissionService.getRoleIds(10L)).thenReturn(List.of(1L, 2L));
        when(userRoleService.getUserIds(List.of(1L, 2L))).thenReturn(List.of(100L, 100L, 200L));

        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.UPDATE,
                10L
        ))).containsExactly(
                PermissionCacheResetCommand.targetPermissions(),
                PermissionCacheResetCommand.publicAccessPermissions(),
                PermissionCacheResetCommand.userPermissions(100L),
                PermissionCacheResetCommand.userPermissions(200L)
        );
    }

    @Test
    void roleAndRolePermissionChangesResetUsersAssignedToTheRole() {
        when(userRoleService.getUserIds(7L)).thenReturn(List.of(100L, 200L));

        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.UPDATE,
                7L
        ))).containsExactly(
                PermissionCacheResetCommand.userPermissions(100L),
                PermissionCacheResetCommand.userPermissions(200L)
        );
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE_PERMISSION,
                RbacAuthzEvent.Action.REPLACE,
                7L
        ))).containsExactly(
                PermissionCacheResetCommand.userPermissions(100L),
                PermissionCacheResetCommand.userPermissions(200L)
        );
    }

    @Test
    void userAndUserRoleChangesResetOnlyThatUser() {
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER,
                RbacAuthzEvent.Action.DELETE,
                100L
        ))).containsExactly(PermissionCacheResetCommand.userPermissions(100L));
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                100L
        ))).containsExactly(PermissionCacheResetCommand.userPermissions(100L));
        verifyNoInteractions(rolePermissionService, userRoleService);
    }

    @Test
    void explicitAllEventProducesOnlyAnAllReset() {
        assertThat(policy.commandsForAll(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR
        ))).containsExactly(PermissionCacheResetCommand.all());
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR,
                (Long) null
        ))).isEmpty();
    }
}
