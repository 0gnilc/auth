package com.gnilc.system.admin.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.entity.vo.AdminVo;
import com.gnilc.system.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {
    private final AdminService service = mock(AdminService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(service)).build();
    }

    @Test
    void loginReturnsTokenOrAuthenticationBusinessError() throws Exception {
        when(service.login("admin", "secret"))
                .thenReturn(AdminTokenVo.of("access", "refresh"));

        mvc.perform(post("/sys/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));

        mvc.perform(post("/sys/admin/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    void refreshAndLogoutUseHttp401ForInvalidRefreshToken() throws Exception {
        when(service.refresh("good")).thenReturn(AdminTokenVo.of("new", "good"));
        when(service.logout("good")).thenReturn(true);

        mvc.perform(post("/sys/admin/refresh").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new"));
        mvc.perform(post("/sys/admin/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002));
        mvc.perform(post("/sys/admin/logout").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk());
        mvc.perform(post("/sys/admin/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileAndAuthorizationReadRoutesPreserveResponseShape() throws Exception {
        AdminVo admin = new AdminVo();
        admin.setUsername("alice");
        when(service.getUserInfo()).thenReturn(admin);
        when(service.getRoleCodes()).thenReturn(List.of("admin"));
        when(service.getMenuAccessCodes()).thenReturn(List.of("user:create"));

        mvc.perform(get("/sys/admin/user-info"))
                .andExpect(jsonPath("$.data.username").value("alice"));
        mvc.perform(get("/sys/admin/role-codes"))
                .andExpect(jsonPath("$.data[0]").value("admin"));
        mvc.perform(get("/sys/admin/menu/access-codes"))
                .andExpect(jsonPath("$.data[0]").value("user:create"));
    }

    @Test
    void currentProfileUpdateAcceptsAdminDtoAndDelegatesToCurrentUserService() throws Exception {
        doNothing().when(service).updateProfile(any());

        mvc.perform(jsonPost("/sys/admin/user-info/update", """
                        {
                          "id": 99,
                          "username": "ignored",
                          "nickname": "Alice",
                          "avatar": "https://example.test/alice.png",
                          "desc": "Platform administrator",
                          "status": false,
                          "roleCodes": ["ignored"]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var captor = org.mockito.ArgumentCaptor.forClass(AdminDto.class);
        verify(service).updateProfile(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("Alice");
        assertThat(captor.getValue().getAvatar()).isEqualTo("https://example.test/alice.png");
        assertThat(captor.getValue().getDesc()).isEqualTo("Platform administrator");
    }

    @Test
    void currentPasswordUpdateAcceptsOnlySimplePasswordParameters() throws Exception {
        mvc.perform(jsonPost("/sys/admin/password/update", """
                        {
                          "id": 99,
                          "oldPassword": "Initial#123",
                          "newPassword": "Changed#456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(service).updatePassword("Initial#123", "Changed#456");
    }

    @Test
    void managementRoutesDelegateAllCommands() throws Exception {
        when(service.getAdminPage(any())).thenReturn(new PageResult<>());

        mvc.perform(post("/sys/admin/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(jsonPost("/sys/admin/create", "{\"username\":\"alice\"}"))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/sys/admin/update", "{\"id\":2}"))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/sys/admin/update-roles", "{\"id\":2,\"roleCodes\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/sys/admin/remove/2")).andExpect(status().isOk());

        verify(service).createAdmin(any());
        verify(service).updateAdmin(any());
        verify(service).updateAdminRoles(any());
        verify(service).removeAdmin(2L);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPost(
            String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
