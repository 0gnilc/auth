package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class UserRoleServiceImplTest extends RbacMessageTestSupport {
    @Test
    void updateUserRoleRejectsMissingAssignmentWithTheDefaultLocale() {
        UserRoleServiceImpl userRoles = new UserRoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                messages());

        assertThatThrownBy(() -> userRoles.updateUserRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("User role assignment information is required.");
    }
}
