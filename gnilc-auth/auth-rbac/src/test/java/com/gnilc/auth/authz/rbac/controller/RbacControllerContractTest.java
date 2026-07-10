package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.config.ServletRbacAuthorizationAutoConfiguration;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RbacControllerContractTest.TestApplication.class,
        properties = "spring.main.web-application-type=servlet"
)
@AutoConfigureMockMvc
@Import(RbacControllerContractTest.EventCaptureConfiguration.class)
class RbacControllerContractTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CapturedEvents capturedEvents;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private MenuService menuService;
    @MockBean
    private RoleMenuService roleMenuService;
    @MockBean
    private RolePermissionService rolePermissionService;
    @MockBean
    private UserRoleService userRoleService;

    @Test
    void permissionListBindsJsonAndReturnsTheRbacResponseEnvelope() throws Exception {
        PermissionVo permission = new PermissionVo();
        permission.setId(7L);
        permission.setCode("account:read");
        permission.setName("Read accounts");
        permission.setTargetIdentifier("/accounts/**");
        permission.setTargetQualifier("GET");
        permission.setPublicAccess(false);
        when(permissionService.getPermissions(argThat((PermissionQueryDto query) ->
                "account:read".equals(query.getCode()) && Boolean.FALSE.equals(query.getPublicAccess()))))
                .thenReturn(List.of(permission));

        mockMvc.perform(post("/authz/permission/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"account:read","publicAccess":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].code").value("account:read"))
                .andExpect(jsonPath("$.data[0].targetIdentifier").value("/accounts/**"));

        verify(permissionService).getPermissions(argThat((PermissionQueryDto query) ->
                "account:read".equals(query.getCode()) && Boolean.FALSE.equals(query.getPublicAccess())));
    }

    @Test
    void cacheClearEndpointPublishesTheDocumentedAllClearEvent() throws Exception {
        capturedEvents.clear();

        mockMvc.perform(post("/authz/permission/cache/clear-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"));

        assertThat(capturedEvents.rbacEvents()).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ALL);
            assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.CLEAR);
            assertThat(event.getData()).isNull();
        });
    }

    @Test
    void permissionMutationRoutesBindBodiesAndPathVariables() throws Exception {
        mockMvc.perform(post("/authz/permission/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"account:write","name":"Write accounts","targetIdentifier":"/accounts/**","publicAccess":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/authz/permission/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":9,"code":"account:edit","name":"Edit accounts","targetIdentifier":"/accounts/**","publicAccess":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/authz/permission/remove/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(permissionService).createPermission(argThat(dto ->
                "account:write".equals(dto.getCode()) && "/accounts/**".equals(dto.getTargetIdentifier())));
        verify(permissionService).updatePermission(argThat(dto ->
                Long.valueOf(9).equals(dto.getId()) && "account:edit".equals(dto.getCode())));
        verify(permissionService).removePermission(9L);
    }

    @Test
    void roleRoutesExposeQueriesAndMutations() throws Exception {
        RoleVo role = new RoleVo();
        role.setId(3L);
        role.setCode("auditor");
        when(roleService.getRoles(argThat((RoleQueryDto query) -> Boolean.FALSE.equals(query.getBuiltIn()))))
                .thenReturn(List.of(role));
        when(roleService.getRolePage(argThat(query ->
                Long.valueOf(2).equals(query.getCurrentPage()) && Long.valueOf(5).equals(query.getPageSize()))))
                .thenReturn(new PageResult<>(List.of(role), 1, 5, 2));

        mockMvc.perform(post("/authz/role/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"builtIn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("auditor"));
        mockMvc.perform(post("/authz/role/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPage\":2,\"pageSize\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPage").value(2))
                .andExpect(jsonPath("$.data.list[0].id").value(3));
        mockMvc.perform(post("/authz/role/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"operator\",\"name\":\"Operator\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/role/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":3,\"code\":\"auditor\",\"name\":\"Auditor\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/role/remove/3"))
                .andExpect(status().isOk());

        verify(roleService).createRole(argThat(dto -> "operator".equals(dto.getCode())));
        verify(roleService).updateRole(argThat(dto -> Long.valueOf(3).equals(dto.getId())));
        verify(roleService).removeRole(3L);
    }

    @Test
    void menuRoutesExposeTreeAndMutations() throws Exception {
        MenuVo menu = new MenuVo();
        menu.setId(5L);
        menu.setName("accounts");
        when(menuService.getMenuTree()).thenReturn(List.of(menu));

        mockMvc.perform(post("/authz/menu/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("accounts"));
        mockMvc.perform(post("/authz/menu/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pid\":0,\"type\":\"button\",\"name\":\"account-create\",\"title\":\"Create\",\"accessCode\":\"account:create\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/menu/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"title\":\"Accounts\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/menu/remove/5"))
                .andExpect(status().isOk());

        verify(menuService).createMenu(argThat(dto ->
                "account-create".equals(dto.getName()) && "account:create".equals(dto.getAccessCode())));
        verify(menuService).updateMenu(argThat(dto -> Long.valueOf(5).equals(dto.getId())));
        verify(menuService).removeMenu(5L);
    }

    @Test
    void relationRoutesExposeCurrentIdsAndReplacementContracts() throws Exception {
        when(roleMenuService.getMenuIds(4L)).thenReturn(List.of(10L, 11L));
        when(rolePermissionService.getPermissionIds(4L)).thenReturn(List.of(20L, 21L));
        when(userRoleService.getRoleIds(7L)).thenReturn(List.of(4L, 5L));

        mockMvc.perform(post("/authz/role-menu/list/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1]").value(11));
        mockMvc.perform(post("/authz/role-permission/list/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(20));
        mockMvc.perform(post("/authz/user-role/list/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1]").value(5));

        mockMvc.perform(post("/authz/role-menu/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":4,\"menuIds\":[10,12]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/role-permission/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":4,\"permissionIds\":[20,22]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authz/user-role/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"roleIds\":[4,6]}"))
                .andExpect(status().isOk());

        verify(roleMenuService).updateRoleMenu(argThat(dto ->
                Long.valueOf(4).equals(dto.getRoleId()) && dto.getMenuIds().equals(List.of(10L, 12L))));
        verify(rolePermissionService).updateRolePermission(argThat(dto ->
                Long.valueOf(4).equals(dto.getRoleId()) && dto.getPermissionIds().equals(List.of(20L, 22L))));
        verify(userRoleService).updateUserRole(argThat(dto ->
                Long.valueOf(7).equals(dto.getUserId()) && dto.getRoleIds().equals(List.of(4L, 6L))));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            ServletRbacAuthorizationAutoConfiguration.class,
            DataSourceAutoConfiguration.class
    })
    static class TestApplication {
        @Bean
        PermissionController permissionController() {
            return new PermissionController();
        }

        @Bean
        RoleController roleController() {
            return new RoleController();
        }

        @Bean
        MenuController menuController() {
            return new MenuController();
        }

        @Bean
        RoleMenuController roleMenuController() {
            return new RoleMenuController();
        }

        @Bean
        RolePermissionController rolePermissionController() {
            return new RolePermissionController();
        }

        @Bean
        UserRoleController userRoleController() {
            return new UserRoleController();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EventCaptureConfiguration {
        @Bean
        CapturedEvents capturedEvents() {
            return new CapturedEvents();
        }
    }

    static final class CapturedEvents {
        private final List<RbacAuthzEvent<?>> events = new ArrayList<>();

        @EventListener
        void capture(RbacAuthzEvent<?> event) {
            events.add(event);
        }

        List<RbacAuthzEvent<?>> rbacEvents() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
