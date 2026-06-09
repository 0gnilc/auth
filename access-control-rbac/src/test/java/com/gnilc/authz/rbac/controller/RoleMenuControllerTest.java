package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.constant.ResponseCode;
import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.authz.rbac.service.RoleMenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleMenuControllerTest {
    private RoleMenuService roleMenuService;
    private RoleMenuController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        roleMenuService = mock(RoleMenuService.class);
        controller = new RoleMenuController();
        ReflectionTestUtils.setField(controller, "roleMenuService", roleMenuService);
    }

    /**
     * Verifies role-menu list requests return menu IDs for a role.
     */
    @Test
    void listReturnsMenuIdsForRole() {
        when(roleMenuService.getMenuIds(1L)).thenReturn(List.of(10L, 20L));

        R<List<Long>> r = controller.getMenuIds(1L);

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(10L, 20L);
    }

    /**
     * Verifies role-menu updates delegate to the service.
     */
    @Test
    void updateDelegatesToService() {
        RoleMenuDto dto = new RoleMenuDto();
        dto.setRoleId(1L);
        dto.setMenuIds(List.of(10L));

        R<?> r = controller.updateRoleMenu(dto);

        verify(roleMenuService).updateRoleMenu(dto);
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }
}
