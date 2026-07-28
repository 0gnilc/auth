package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RolePermissionServiceImplTest extends RbacMessageTestSupport {
    @Test
    void saveRolePermissionsRejectsMissingAssignmentWithTheDefaultLocale() {
        RolePermissionServiceImpl rolePermissions = new RolePermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(PermissionService.class),
                mock(RoleService.class),
                messages());

        assertThatThrownBy(() -> rolePermissions.saveRolePermissions(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role permission assignment information is required.");
    }

    @Test
    void saveRolePermissionsRejectsBuiltInRolesBeforeReadingBindings() {
        RoleService roles = mock(RoleService.class);
        RolePermissionServiceImpl rolePermissions = new RolePermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(PermissionService.class),
                roles,
                messages());
        RoleBo builtIn = new RoleBo();
        builtIn.setId(7L);
        builtIn.setBuiltIn(true);
        when(roles.getById(7L)).thenReturn(builtIn);
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleId(7L);

        assertThatThrownBy(() -> rolePermissions.saveRolePermissions(dto))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in role permissions and menus cannot be modified.");
        verify(roles).getById(7L);
        verify(roles, never()).updateById(builtIn);
    }

    @Test
    void saveRolePermissionsRejectsPermissionsThatNoLongerExist() {
        PermissionService permissions = mock(PermissionService.class);
        RoleService roles = mock(RoleService.class);
        RoleBo role = new RoleBo();
        role.setBuiltIn(false);
        when(roles.getById(7L)).thenReturn(role);
        PermissionBo existing = new PermissionBo();
        existing.setId(11L);
        when(permissions.getPermissions(anyList())).thenReturn(java.util.List.of(existing));
        RolePermissionServiceImpl rolePermissions = new RolePermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                permissions,
                roles,
                messages());
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleId(7L);
        dto.setPermissionIds(java.util.List.of(11L, 12L));

        assertThatThrownBy(() -> rolePermissions.saveRolePermissions(dto))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The permission no longer exists. Refresh and try again.");
    }
}
