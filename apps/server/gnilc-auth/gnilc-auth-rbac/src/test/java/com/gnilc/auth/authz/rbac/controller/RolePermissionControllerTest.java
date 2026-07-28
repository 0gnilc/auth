package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.gnilc.auth.authz.rbac.controller.ControllerTestRequests.jsonPost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolePermissionControllerTest {
    @Test
    void exposesCurrentIdsAndReplaceCommand() throws Exception {
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RolePermissionController(rolePermissions)).build();
        when(rolePermissions.getPermissionIds(2L)).thenReturn(List.of(5L));

        mvc.perform(post("/authz/role-permission/list/2")).andExpect(jsonPath("$.data[0]").value(5));
        mvc.perform(jsonPost("/authz/role-permission/save",
                "{\"roleId\":2,\"permissionIds\":[5]}"))
                .andExpect(status().isOk());
    }
}
