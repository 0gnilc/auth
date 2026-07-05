package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionControllerTest {
    private PermissionService permissionService;
    private ApplicationEventPublisher publisher;
    private PermissionController controller;

    /**
     * Sets up fresh controller fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        publisher = mock(ApplicationEventPublisher.class);
        controller = new PermissionController();
        ReflectionTestUtils.setField(controller, "permissionService", permissionService);
        ReflectionTestUtils.setField(controller, "publisher", publisher);
    }

    /**
     * Verifies permission list requests return service data.
     */
    // TestCaseId: RBAC-CONTROLLER-009
    @Test
    void listReturnsPermissionsFromService() {
        PermissionQueryDto query = new PermissionQueryDto();
        PermissionVo permission = new PermissionVo();
        permission.setId(1L);
        permission.setCode("user:read");
        when(permissionService.getPermissions(query)).thenReturn(List.of(permission));

        R<List<PermissionVo>> r = controller.getPermissions(query);

        assertSuccess(r);
        assertThat(r.getData()).containsExactly(permission);
    }

    /**
     * Verifies permission creation delegates to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-010
    @Test
    void createDelegatesToServiceAndReturnsSuccess() {
        PermissionDto dto = new PermissionDto();
        dto.setCode("user:read");

        R<?> r = controller.createPermission(dto);

        verify(permissionService).createPermission(dto);
        assertSuccess(r);
    }

    /**
     * Verifies permission updates delegate to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-019
    @Test
    void updateDelegatesToServiceAndReturnsSuccess() {
        PermissionDto dto = new PermissionDto();
        dto.setId(1L);

        R<?> r = controller.updatePermission(dto);

        verify(permissionService).updatePermission(dto);
        assertSuccess(r);
    }

    /**
     * Verifies permission removal passes the path ID to the service.
     */
    // TestCaseId: RBAC-CONTROLLER-011
    @Test
    void removeDelegatesPathIdToServiceAndReturnsSuccess() {
        R<?> r = controller.removePermission(1L);

        verify(permissionService).removePermission(1L);
        assertSuccess(r);
    }

    /**
     * Verifies clearing all permission cache publishes the expected event.
     */
    // TestCaseId: RBAC-CONTROLLER-020
    @Test
    void clearAllPermissionCachePublishesAllClearedEvent() {
        R<?> r = controller.clearAllPermissionCache();

        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ALL);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.CLEAR);
        assertThat(event.getData()).isNull();
        assertThat(event.getExtra()).isNull();
        assertSuccess(r);
    }

    private void assertSuccess(R<?> r) {
        assertThat(r.getCode()).isEqualTo(ResponseCode.SUCCESS.getBusinessCode());
        assertThat(r.getMessage()).isEqualTo(ResponseCode.SUCCESS.getMessage());
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
