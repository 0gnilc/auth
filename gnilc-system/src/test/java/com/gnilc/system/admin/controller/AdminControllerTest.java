package com.gnilc.system.admin.controller;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.entity.vo.AdminVo;
import com.gnilc.system.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(AdminController.class)
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    void pageUsesJsonFiltersAndReturnsTheServicePage() throws Exception {
        AdminVo admin = new AdminVo();
        admin.setId(20L);
        admin.setUserId(1001L);
        admin.setUsername("admin");
        admin.setRoleCodes(List.of("admin", "operator"));
        when(adminService.getAdminPage(any(AdminPageDto.class)))
                .thenReturn(new PageResult<>(List.of(admin), 1, 10, 1));

        mockMvc.perform(post("/sys/admin/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPage":1,"pageSize":10,"username":"admin","nickname":"管","status":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                .andExpect(jsonPath("$.data.list[0].roleCodes[1]").value("operator"));

        ArgumentCaptor<AdminPageDto> captor = ArgumentCaptor.forClass(AdminPageDto.class);
        verify(adminService).getAdminPage(captor.capture());
        assertThat(captor.getValue().getCurrentPage()).isEqualTo(1L);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10L);
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getNickname()).isEqualTo("管");
        assertThat(captor.getValue().getStatus()).isTrue();
    }

    @Test
    void managementRoutesBindTheirPublicRequestContracts() throws Exception {
        mockMvc.perform(post("/sys/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new-admin","password":"Strong1!","nickname":"New","roleCodes":["operator"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        ArgumentCaptor<AdminDto> adminCaptor = ArgumentCaptor.forClass(AdminDto.class);
        verify(adminService).createAdmin(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getUsername()).isEqualTo("new-admin");
        assertThat(adminCaptor.getValue().getRoleCodes()).containsExactly("operator");

        mockMvc.perform(post("/sys/admin/update-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":10,\"roleCodes\":[]}"))
                .andExpect(status().isOk());
        ArgumentCaptor<AdminRoleDto> roleCaptor = ArgumentCaptor.forClass(AdminRoleDto.class);
        verify(adminService).updateAdminRoles(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(roleCaptor.getValue().getRoleCodes()).isEmpty();

        mockMvc.perform(post("/sys/admin/remove/10"))
                .andExpect(status().isOk());
        verify(adminService).removeAdmin(10L);
    }

    @Test
    void loginSuccessReturnsTokensAtHttp200() throws Exception {
        when(adminService.login("admin", "secret")).thenReturn(AdminTokenVo.of("access", "refresh"));

        mockMvc.perform(post("/sys/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void loginFailureIsAnHttp200BusinessFailure() throws Exception {
        when(adminService.login("admin", "bad-secret")).thenReturn(null);

        mockMvc.perform(post("/sys/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.AUTHENTICATION_FAILED.getBusinessCode()))
                .andExpect(jsonPath("$.error").value("用户名或密码错误"));
    }

    @Test
    void invalidRefreshAndLogoutAreHttp401WithUnauthorizedBusinessCode() throws Exception {
        mockMvc.perform(post("/sys/admin/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(401)));

        when(adminService.logout("unknown-refresh")).thenReturn(false);
        mockMvc.perform(post("/sys/admin/logout")
                        .header("X-Refresh-Token", "unknown-refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()));
    }

    @Test
    void refreshLogoutAndCurrentUserQueriesUseTheServiceContracts() throws Exception {
        when(adminService.refresh("refresh-token"))
                .thenReturn(AdminTokenVo.of("new-access", "refresh-token"));
        when(adminService.logout("refresh-token")).thenReturn(true);
        AdminVo profile = new AdminVo();
        profile.setUsername("admin");
        profile.setRoleCodes(List.of("admin"));
        when(adminService.getUserInfo()).thenReturn(profile);
        when(adminService.getRoleCodes()).thenReturn(List.of("admin"));
        when(adminService.getMenuAccessCodes()).thenReturn(List.of("admin:create"));

        mockMvc.perform(post("/sys/admin/refresh").header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
        mockMvc.perform(post("/sys/admin/logout").header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/sys/admin/user-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
        mockMvc.perform(get("/sys/admin/role-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("admin"));
        mockMvc.perform(get("/sys/admin/menu/access-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("admin:create"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
