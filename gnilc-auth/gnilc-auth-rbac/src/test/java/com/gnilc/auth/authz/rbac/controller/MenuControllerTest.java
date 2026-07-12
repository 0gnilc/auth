package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import org.junit.jupiter.api.Test;
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

class MenuControllerTest {
    @Test
    void exposesTreeAndMutations() throws Exception {
        MenuService menus = mock(MenuService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MenuController(menus)).build();
        MenuVo menu = new MenuVo();
        menu.setName("root");
        when(menus.getMenuTree()).thenReturn(List.of(menu));

        mvc.perform(post("/authz/menu/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("root"));
        mvc.perform(jsonPost("/authz/menu/create", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/menu/update", "{\"id\":1}")).andExpect(status().isOk());
        mvc.perform(post("/authz/menu/remove/1")).andExpect(status().isOk());

        verify(menus).createMenu(any());
        verify(menus).updateMenu(any());
        verify(menus).removeMenu(1L);
    }
}
