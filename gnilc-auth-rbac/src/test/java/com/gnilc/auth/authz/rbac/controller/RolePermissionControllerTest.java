package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RolePermissionControllerTest {
    private RolePermissionService rolePermissionService;
    private RolePermissionController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        rolePermissionService = mock(RolePermissionService.class);
        controller = new RolePermissionController();
        ReflectionTestUtils.setField(controller, "rolePermissionService", rolePermissionService);
    }

    /**
     * Verifies role-permission list requests return permission IDs for a role.
     */
    // TestCaseId: RBAC-CONTROLLER-006
    @Test
    void listReturnsPermissionIdsForRole() {
        when(rolePermissionService.getPermissionIds(1L)).thenReturn(List.of(10L, 20L));

        R<List<Long>> r = controller.getPermissionIds(1L);

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(10L, 20L);
    }

    /**
     * Verifies role-permission updates delegate to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-016
    @Test
    void updateDelegatesToService() {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleId(1L);
        dto.setPermissionIds(List.of(10L));

        R<?> r = controller.updateRolePermission(dto);

        verify(rolePermissionService).updateRolePermission(dto);
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getBusinessCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }
}
