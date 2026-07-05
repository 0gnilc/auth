package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import com.gnilc.auth.authz.rbac.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleControllerTest {
    private RoleService roleService;
    private RoleController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        controller = new RoleController();
        ReflectionTestUtils.setField(controller, "roleService", roleService);
    }

    /**
     * Verifies role page requests return service data.
     */
    // TestCaseId: RBAC-CONTROLLER-002
    @Test
    void pageReturnsServicePage() {
        RolePageDto dto = new RolePageDto();
        PageResult<RoleVo> page = new PageResult<>();
        when(roleService.getRolePage(dto)).thenReturn(page);

        R<PageResult<RoleVo>> r = controller.getRolePage(dto);

        assertSuccess(r);
        assertThat(r.getData()).isSameAs(page);
    }

    /**
     * Verifies role list requests return service data.
     */
    // TestCaseId: RBAC-CONTROLLER-013
    @Test
    void listReturnsServiceRoles() {
        RoleQueryDto dto = new RoleQueryDto();
        RoleVo role = new RoleVo();
        role.setCode("admin");
        when(roleService.getRoles(dto)).thenReturn(List.of(role));

        R<List<RoleVo>> r = controller.getRoles(dto);

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(role);
    }

    /**
     * Verifies role creation delegates to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-003
    @Test
    void createDelegatesToService() {
        RoleDto dto = new RoleDto();
        dto.setCode("admin");

        R<?> r = controller.createRole(dto);

        verify(roleService).createRole(dto);
        assertSuccess(r);
    }

    /**
     * Verifies role updates delegate to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-014
    @Test
    void updateDelegatesToService() {
        RoleDto dto = new RoleDto();
        dto.setId(1L);

        R<?> r = controller.updateRole(dto);

        verify(roleService).updateRole(dto);
        assertSuccess(r);
    }

    /**
     * Verifies role removal passes the path ID to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-004
    @Test
    void removeDelegatesPathIdToService() {
        R<?> r = controller.removeRole(1L);

        verify(roleService).removeRole(1L);
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getBusinessCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }
}
