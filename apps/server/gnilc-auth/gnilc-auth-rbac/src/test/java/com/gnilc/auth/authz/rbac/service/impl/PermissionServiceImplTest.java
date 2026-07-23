package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
}
