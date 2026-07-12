package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.service.UserRoleService;
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

class UserRoleControllerTest {
    @Test
    void exposesCurrentIdsAndReplaceCommand() throws Exception {
        UserRoleService userRoles = mock(UserRoleService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserRoleController(userRoles)).build();
        when(userRoles.getRoleIds(7L)).thenReturn(List.of(2L));

        mvc.perform(post("/authz/user-role/list/7")).andExpect(jsonPath("$.data[0]").value(2));
        mvc.perform(jsonPost("/authz/user-role/update", "{\"userId\":7,\"roleIds\":[2]}"))
                .andExpect(status().isOk());
    }
}
