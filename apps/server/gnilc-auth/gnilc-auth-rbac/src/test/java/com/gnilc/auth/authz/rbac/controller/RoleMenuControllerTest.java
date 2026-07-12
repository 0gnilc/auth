package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.service.RoleMenuService;
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

class RoleMenuControllerTest {
    @Test
    void exposesCurrentIdsAndReplaceCommand() throws Exception {
        RoleMenuService roleMenus = mock(RoleMenuService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RoleMenuController(roleMenus)).build();
        when(roleMenus.getMenuIds(2L)).thenReturn(List.of(4L));

        mvc.perform(post("/authz/role-menu/list/2")).andExpect(jsonPath("$.data[0]").value(4));
        mvc.perform(jsonPost("/authz/role-menu/update", "{\"roleId\":2,\"menuIds\":[4]}"))
                .andExpect(status().isOk());
    }
}
