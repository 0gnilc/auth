package com.gnilc.system.admin.controller;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.entity.vo.AdminVo;
import com.gnilc.system.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {
    private AdminService adminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService)).build();
    }

    // TestCaseId: SYS-ADMIN-001
    @Test
    void adminPageBindsFiltersAndReturnsPagedProfiles() throws Exception {
        AdminVo admin = new AdminVo();
        admin.setId(20L);
        admin.setUserId(1001L);
        admin.setUsername("admin");
        admin.setNickname("管理员");
        admin.setAvatar("avatar.png");
        admin.setDesc("desc");
        admin.setHomePath("/workspace");
        admin.setStatus(true);
        admin.setRoleCodes(List.of("admin", "operator"));
        when(adminService.getAdminPage(any(AdminPageDto.class)))
                .thenReturn(new PageResult<>(List.of(admin), 1, 10, 1));

        mockMvc.perform(post("/sys/admin/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPage\":1,\"pageSize\":10,\"username\":\"admin\",\"nickname\":\"管\",\"status\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(20))
                .andExpect(jsonPath("$.data.list[0].userId").value(1001))
                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                .andExpect(jsonPath("$.data.list[0].nickname").value("管理员"))
                .andExpect(jsonPath("$.data.list[0].avatar").value("avatar.png"))
                .andExpect(jsonPath("$.data.list[0].desc").value("desc"))
                .andExpect(jsonPath("$.data.list[0].homePath").value("/workspace"))
                .andExpect(jsonPath("$.data.list[0].status").value(true))
                .andExpect(jsonPath("$.data.list[0].roleCodes[1]").value("operator"));

        ArgumentCaptor<AdminPageDto> dtoCaptor = ArgumentCaptor.forClass(AdminPageDto.class);
        verify(adminService).getAdminPage(dtoCaptor.capture());
        AdminPageDto dto = dtoCaptor.getValue();
        assertThat(dto.getCurrentPage()).isEqualTo(1L);
        assertThat(dto.getPageSize()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("admin");
        assertThat(dto.getNickname()).isEqualTo("管");
        assertThat(dto.getStatus()).isTrue();
    }

    // TestCaseId: SYS-ADMIN-002
    @Test
    void createAdminDelegatesProfilePayload() throws Exception {
        mockMvc.perform(post("/sys/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Strong1!\",\"nickname\":\"管理员\",\"desc\":\"desc\",\"roleCodes\":[\"admin\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AdminDto> dtoCaptor = ArgumentCaptor.forClass(AdminDto.class);
        verify(adminService).createAdmin(dtoCaptor.capture());
        AdminDto dto = dtoCaptor.getValue();
        assertThat(dto.getUsername()).isEqualTo("admin");
        assertThat(dto.getPassword()).isEqualTo("Strong1!");
        assertThat(dto.getDesc()).isEqualTo("desc");
        assertThat(dto.getRoleCodes()).containsExactly("admin");
    }

    // TestCaseId: SYS-ADMIN-003
    @Test
    void updateAdminDelegatesPartialPayload() throws Exception {
        mockMvc.perform(post("/sys/admin/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":10,\"nickname\":\"新昵称\",\"roleCodes\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AdminDto> dtoCaptor = ArgumentCaptor.forClass(AdminDto.class);
        verify(adminService).updateAdmin(dtoCaptor.capture());
        AdminDto dto = dtoCaptor.getValue();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getNickname()).isEqualTo("新昵称");
        assertThat(dto.getRoleCodes()).isEmpty();
    }

    // TestCaseId: SYS-ADMIN-004
    @Test
    void updateRolesAndRemoveDelegateToAdminService() throws Exception {
        mockMvc.perform(post("/sys/admin/update-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":10,\"roleCodes\":[\"operator\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        ArgumentCaptor<AdminRoleDto> roleDtoCaptor = ArgumentCaptor.forClass(AdminRoleDto.class);
        verify(adminService).updateAdminRoles(roleDtoCaptor.capture());
        assertThat(roleDtoCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(roleDtoCaptor.getValue().getRoleCodes()).containsExactly("operator");

        mockMvc.perform(post("/sys/admin/remove/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(adminService).removeAdmin(10L);
    }

    // TestCaseId: SYS-ADMIN-005
    @Test
    void loginExtractsCredentialsAndReturnsTokens() throws Exception {
        when(adminService.login("admin", "secret")).thenReturn(AdminTokenVo.of("access", "refresh"));

        mockMvc.perform(post("/sys/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\",\"ignored\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));

        verify(adminService).login("admin", "secret");
    }

    // TestCaseId: SYS-ADMIN-006
    @Test
    void loginFailureReturnsBusinessResultNotHttpUnauthorized() throws Exception {
        when(adminService.login("admin", "bad-secret")).thenReturn(null);

        mockMvc.perform(post("/sys/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.AUTHENTICATION_FAILED.getBusinessCode()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").value("用户名或密码错误"));
    }

    // TestCaseId: SYS-ADMIN-007
    @Test
    void loginMissingBodyReturnsBusinessResultNotTransportFailure() throws Exception {
        mockMvc.perform(post("/sys/admin/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.AUTHENTICATION_FAILED.getBusinessCode()))
                .andExpect(jsonPath("$.error").value("用户名或密码错误"));
    }

    // TestCaseId: SYS-ADMIN-008
    @Test
    void refreshUsesRefreshHeaderAndReturnsTokens() throws Exception {
        when(adminService.refresh("refresh-token")).thenReturn(AdminTokenVo.of("new-access", "new-refresh"));

        mockMvc.perform(post("/sys/admin/refresh").header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    // TestCaseId: SYS-ADMIN-009
    @Test
    void refreshMissingOrInvalidTokenReturnsUnauthorizedResponseEntity() throws Exception {
        mockMvc.perform(post("/sys/admin/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()));

        when(adminService.refresh("bad-refresh")).thenReturn(null);

        mockMvc.perform(post("/sys/admin/refresh").header("X-Refresh-Token", "bad-refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()));
    }

    // TestCaseId: SYS-ADMIN-010
    @Test
    void logoutUsesRefreshHeader() throws Exception {
        when(adminService.logout("refresh-token")).thenReturn(true);

        mockMvc.perform(post("/sys/admin/logout").header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // TestCaseId: SYS-ADMIN-011
    @Test
    void logoutMissingOrInvalidTokenReturnsUnauthorizedResponseEntity() throws Exception {
        mockMvc.perform(post("/sys/admin/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()));

        when(adminService.logout("bad-refresh")).thenReturn(false);

        mockMvc.perform(post("/sys/admin/logout").header("X-Refresh-Token", "bad-refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getBusinessCode()));
    }

    // TestCaseId: SYS-ADMIN-012
    @Test
    void userInfoReturnsSessionProfileWithRoleCodesAndNoTokenOrStatus() throws Exception {
        AdminVo userInfo = new AdminVo();
        userInfo.setId(10L);
        userInfo.setUserId(1001L);
        userInfo.setUsername("admin");
        userInfo.setNickname("管理员");
        userInfo.setRoleCodes(List.of("admin", "operator"));
        when(adminService.getUserInfo()).thenReturn(userInfo);

        mockMvc.perform(get("/sys/admin/user-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("admin"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }

    // TestCaseId: SYS-ADMIN-013
    @Test
    void roleCodesReturnsSessionRoleCodes() throws Exception {
        when(adminService.getRoleCodes()).thenReturn(List.of("admin", "operator"));

        mockMvc.perform(get("/sys/admin/role-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("admin"))
                .andExpect(jsonPath("$.data[1]").value("operator"));
    }

    // TestCaseId: SYS-ADMIN-014
    @Test
    void menuAccessCodesReturnsSessionButtonAccessCodes() throws Exception {
        when(adminService.getMenuAccessCodes()).thenReturn(List.of("user:create", "user:update"));

        mockMvc.perform(get("/sys/admin/menu/access-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("user:create"))
                .andExpect(jsonPath("$.data[1]").value("user:update"));
    }
}
