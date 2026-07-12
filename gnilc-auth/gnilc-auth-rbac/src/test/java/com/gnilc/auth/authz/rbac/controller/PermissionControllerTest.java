package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.gnilc.auth.authz.rbac.controller.ControllerTestRequests.jsonPost;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionControllerTest {
    @Test
    void exposesQueriesMutationsAndCacheReset() throws Exception {
        PermissionService permissions = mock(PermissionService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PermissionController(permissions, publisher)).build();
        PermissionVo permission = new PermissionVo();
        permission.setCode("read");
        when(permissions.getPermissions(any(PermissionQueryDto.class))).thenReturn(List.of(permission));

        mvc.perform(jsonPost("/authz/permission/list", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("read"));
        mvc.perform(jsonPost("/authz/permission/create", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/permission/update", "{\"id\":2}")).andExpect(status().isOk());
        mvc.perform(post("/authz/permission/remove/2")).andExpect(status().isOk());
        mvc.perform(post("/authz/permission/cache/clear-all")).andExpect(status().isOk());

        verify(permissions).removePermission(2L);
        verify(publisher).publishEvent(any(Object.class));
    }
}
