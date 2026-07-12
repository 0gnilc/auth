package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import com.gnilc.auth.authz.rbac.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.gnilc.auth.authz.rbac.controller.ControllerTestRequests.jsonPost;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerTest {
    @Test
    void exposesListPageAndMutations() throws Exception {
        RoleService roles = mock(RoleService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RoleController(roles)).build();
        RoleVo role = new RoleVo();
        role.setCode("admin");
        when(roles.getRoles(any(RoleQueryDto.class))).thenReturn(List.of(role));

        mvc.perform(jsonPost("/authz/role/list", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("admin"));
        mvc.perform(jsonPost("/authz/role/page", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/role/create", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/role/update", "{\"id\":3}")).andExpect(status().isOk());
        mvc.perform(post("/authz/role/remove/3")).andExpect(status().isOk());
    }
}
