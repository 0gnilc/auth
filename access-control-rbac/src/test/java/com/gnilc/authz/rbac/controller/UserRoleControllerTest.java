package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.constant.ResponseCode;
import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRoleControllerTest {
    private UserRoleService userRoleService;
    private UserRoleController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        userRoleService = mock(UserRoleService.class);
        controller = new UserRoleController();
        ReflectionTestUtils.setField(controller, "userRoleService", userRoleService);
    }

    /**
     * Verifies user-role list requests return role IDs for a user.
     */
    @Test
    void listReturnsRoleIdsForUser() {
        when(userRoleService.getRoleIds(100L)).thenReturn(List.of(1L, 2L));

        R<List<Long>> r = controller.getRoleIds(100L);

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(1L, 2L);
    }

    /**
     * Verifies user-role updates delegate to the service.
     */
    @Test
    void updateDelegatesToService() {
        UserRoleDto dto = new UserRoleDto();
        dto.setUserId(100L);
        dto.setRoleIds(List.of(1L));

        R<?> r = controller.updateUserRole(dto);

        verify(userRoleService).updateUserRole(dto);
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }
}
