package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacControllerTest {
    private final MenuService menus = mock(MenuService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final RoleService roles = mock(RoleService.class);
    private final RoleMenuService roleMenus = mock(RoleMenuService.class);
    private final RolePermissionService rolePermissions = mock(RolePermissionService.class);
    private final UserRoleService userRoles = mock(UserRoleService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MenuController menuController = new MenuController();
        PermissionController permissionController = new PermissionController();
        RoleController roleController = new RoleController();
        RoleMenuController roleMenuController = new RoleMenuController();
        RolePermissionController rolePermissionController = new RolePermissionController();
        UserRoleController userRoleController = new UserRoleController();
        ReflectionTestUtils.setField(menuController, "menuService", menus);
        ReflectionTestUtils.setField(permissionController, "permissionService", permissions);
        ReflectionTestUtils.setField(permissionController, "publisher", publisher);
        ReflectionTestUtils.setField(roleController, "roleService", roles);
        ReflectionTestUtils.setField(roleMenuController, "roleMenuService", roleMenus);
        ReflectionTestUtils.setField(rolePermissionController, "rolePermissionService", rolePermissions);
        ReflectionTestUtils.setField(userRoleController, "userRoleService", userRoles);
        mvc = MockMvcBuilders.standaloneSetup(menuController, permissionController, roleController,
                roleMenuController, rolePermissionController, userRoleController).build();
    }

    @Test
    void menuRoutesExposeTreeAndMutations() throws Exception {
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

    @Test
    void permissionRoutesExposeQueriesMutationsAndCacheReset() throws Exception {
        PermissionVo permission = new PermissionVo();
        permission.setCode("read");
        when(permissions.getPermissions(
                any(com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto.class)))
                .thenReturn(List.of(permission));

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

    @Test
    void roleRoutesExposeListPageAndMutations() throws Exception {
        RoleVo role = new RoleVo();
        role.setCode("admin");
        when(roles.getRoles(any(com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto.class)))
                .thenReturn(List.of(role));

        mvc.perform(jsonPost("/authz/role/list", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("admin"));
        mvc.perform(jsonPost("/authz/role/page", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/role/create", "{}")).andExpect(status().isOk());
        mvc.perform(jsonPost("/authz/role/update", "{\"id\":3}")).andExpect(status().isOk());
        mvc.perform(post("/authz/role/remove/3")).andExpect(status().isOk());
    }

    @Test
    void relationshipRoutesExposeCurrentIdsAndReplaceCommands() throws Exception {
        when(roleMenus.getMenuIds(2L)).thenReturn(List.of(4L));
        when(rolePermissions.getPermissionIds(2L)).thenReturn(List.of(5L));
        when(userRoles.getRoleIds(7L)).thenReturn(List.of(2L));

        mvc.perform(post("/authz/role-menu/list/2"))
                .andExpect(jsonPath("$.data[0]").value(4));
        mvc.perform(jsonPost("/authz/role-menu/update", "{\"roleId\":2,\"menuIds\":[4]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/authz/role-permission/list/2"))
                .andExpect(jsonPath("$.data[0]").value(5));
        mvc.perform(jsonPost("/authz/role-permission/update",
                "{\"roleId\":2,\"permissionIds\":[5]}")).andExpect(status().isOk());
        mvc.perform(post("/authz/user-role/list/7"))
                .andExpect(jsonPath("$.data[0]").value(2));
        mvc.perform(jsonPost("/authz/user-role/update", "{\"userId\":7,\"roleIds\":[2]}"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPost(
            String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
