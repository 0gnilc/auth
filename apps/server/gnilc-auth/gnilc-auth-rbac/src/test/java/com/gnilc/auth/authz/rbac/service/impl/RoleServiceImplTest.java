package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RoleServiceImplTest extends RbacMessageTestSupport {
    @Test
    void createRoleRejectsMissingInformationWithTheDefaultLocale() {
        RoleServiceImpl roles = new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                messages());

        assertThatThrownBy(() -> roles.createRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role information is required.");
    }
}
