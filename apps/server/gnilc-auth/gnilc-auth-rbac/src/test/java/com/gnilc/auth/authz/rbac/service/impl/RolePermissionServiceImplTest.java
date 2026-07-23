package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RolePermissionServiceImplTest extends RbacMessageTestSupport {
    @Test
    void updateRolePermissionRejectsMissingAssignmentWithTheDefaultLocale() {
        RolePermissionServiceImpl rolePermissions = new RolePermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                messages());

        assertThatThrownBy(() -> rolePermissions.updateRolePermission(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role permission assignment information is required.");
    }
}
