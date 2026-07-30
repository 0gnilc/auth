package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RoleServiceImplTest extends RbacMessageTestSupport {
    @Test
    void createRoleRejectsMissingInformationWithTheDefaultLocale() {
        RoleServiceImpl roles = new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                mock(RoleMenuService.class),
                messages());

        assertThatThrownBy(() -> roles.createRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role information is required.");
    }

    @Test
    void createRoleRejectsWhitespaceOnlyNames() {
        RoleServiceImpl roles = new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                mock(RoleMenuService.class),
                messages());
        RoleDto dto = new RoleDto();
        dto.setCode("operator");
        dto.setName("   ");

        assertThatThrownBy(() -> roles.createRole(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role name is required.");
    }

    @Test
    void createRoleRejectsFieldsBeyondDatabaseLimits() {
        RoleServiceImpl roles = new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                mock(RoleMenuService.class),
                messages());
        RoleDto dto = new RoleDto();
        dto.setCode("r".repeat(256));
        dto.setName("Oversized role");

        assertThatThrownBy(() -> roles.createRole(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role code must not exceed 255 characters.");
    }

    @Test
    void removeRoleClearsAllRelationships() {
        UserRoleService userRoles = mock(UserRoleService.class);
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        RoleMenuService roleMenus = mock(RoleMenuService.class);
        RoleServiceImpl roles = spy(new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                userRoles,
                rolePermissions,
                roleMenus,
                messages()));
        RoleBo role = new RoleBo();
        role.setId(7L);
        String originalCode = "\uD83D\uDE00".repeat(255);
        role.setCode(originalCode);
        role.setBuiltIn(false);
        doReturn(role).when(roles).getById(7L);
        doReturn(true).when(roles).updateById(role);
        doReturn(true).when(roles).removeById(7L);

        roles.removeRole(7L);

        verify(rolePermissions).removeByRoleId(7L);
        verify(roleMenus).removeByRoleId(7L);
        verify(userRoles).removeByRoleId(7L);
        verify(roles).removeById(7L);
        assertThat(role.getCode()).isEqualTo(originalCode + "_del_7");
    }
}
