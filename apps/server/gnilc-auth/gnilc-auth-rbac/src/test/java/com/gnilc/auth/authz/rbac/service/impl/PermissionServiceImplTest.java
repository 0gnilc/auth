package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PermissionServiceImplTest extends RbacMessageTestSupport {
    @Test
    void createPermissionRejectsMissingInformationWithTheDefaultLocale() {
        PermissionServiceImpl permissions = new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                messages());

        assertThatThrownBy(() -> permissions.createPermission(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Permission information is required.");
    }

    @Test
    void updateAndRemoveRejectBuiltInPermissions() {
        PermissionServiceImpl permissions = spy(new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                messages()));
        PermissionBo builtIn = new PermissionBo();
        builtIn.setId(1L);
        builtIn.setBuiltIn(true);
        doReturn(builtIn).when(permissions).getById(1L);
        PermissionDto update = new PermissionDto();
        update.setId(1L);

        assertThatThrownBy(() -> permissions.updatePermission(update))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in permissions cannot be modified.");
        assertThatThrownBy(() -> permissions.removePermission(1L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in permissions cannot be deleted.");
    }

    @Test
    void removePermissionClearsRoleBindings() {
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        PermissionServiceImpl permissions = spy(new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                rolePermissions,
                messages()));
        PermissionBo permission = new PermissionBo();
        permission.setId(2L);
        permission.setCode("report:read");
        permission.setBuiltIn(false);
        doReturn(permission).when(permissions).getById(2L);
        doReturn(true).when(permissions).updateById(permission);
        doReturn(true).when(permissions).removeById(2L);

        permissions.removePermission(2L);

        verify(rolePermissions).removeByPermissionId(2L);
        verify(permissions).removeById(2L);
    }
}
