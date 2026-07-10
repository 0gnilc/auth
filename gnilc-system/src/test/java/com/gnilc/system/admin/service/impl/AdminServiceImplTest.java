package com.gnilc.system.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.system.session.AdminSessionManager;
import com.gnilc.system.session.AdminSessionTokenPair;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.entity.vo.AdminVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceImplTest {
    private AdminSessionManager tokenSessionService;
    private BCryptPasswordEncoder passwordEncoder;
    private RoleService roleService;
    private MenuService menuService;
    private UserService userService;
    private UserRoleService userRoleService;
    private LambdaQueryChainWrapper<AdminBo> query;
    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        tokenSessionService = mock(AdminSessionManager.class);
        passwordEncoder = new BCryptPasswordEncoder();
        roleService = mock(RoleService.class);
        menuService = mock(MenuService.class);
        userService = mock(UserService.class);
        userRoleService = mock(UserRoleService.class);
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<AdminBo, ?>>any(), any());
        doReturn(query).when(query).eq(any(Boolean.class), ArgumentMatchers.<SFunction<AdminBo, ?>>any(), any());
        doReturn(query).when(query).like(any(Boolean.class), ArgumentMatchers.<SFunction<AdminBo, ?>>any(), any());
        doReturn(query).when(query).orderByDesc(ArgumentMatchers.<SFunction<AdminBo, ?>>any());
        service = spy(new AdminServiceImpl(tokenSessionService, roleService, menuService, userService, userRoleService));
        doReturn(query).when(service).lambdaQuery();
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // TestCaseId: SYS-ADMIN-016
    @Test
    void getAdminByUsernameReturnsMatchingAdminAndSkipsBlankUsername() {
        AdminBo admin = enabledAdmin();
        when(query.one()).thenReturn(admin);

        assertThat(service.getAdminByUsername("admin")).isSameAs(admin);
        assertThat(service.getAdminByUsername(" ")).isNull();
    }

    // TestCaseId: SYS-ADMIN-017
    @Test
    void updateAdminRolesReplacesFullRoleSetAndDistinguishesInvalidCodes() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        doReturn(existing).when(service).getById(10L);
        when(roleService.getRoleByCode("operator")).thenReturn(role("operator", 2L));
        when(roleService.getRoleByCode("missing")).thenReturn(null);

        service.updateAdminRoles(adminRoleDto(10L, List.of("operator")));

        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(userRoleCaptor.getValue().getRoleIds()).containsExactly(2L);

        assertThatThrownBy(() -> service.updateAdminRoles(adminRoleDto(10L, List.of(" "))))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("角色标识不能为空");
        assertThatThrownBy(() -> service.updateAdminRoles(adminRoleDto(10L, List.of("missing"))))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("角色不存在，请刷新后重试");
    }

    // TestCaseId: SYS-ADMIN-018
    @Test
    void updateAdminRolesClearsFullRoleSetWhenRoleCodesAreNullOrEmpty() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        doReturn(existing).when(service).getById(10L);

        service.updateAdminRoles(adminRoleDto(10L, null));
        service.updateAdminRoles(adminRoleDto(10L, List.of()));

        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService, org.mockito.Mockito.times(2)).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getAllValues())
                .allSatisfy(dto -> {
                    assertThat(dto.getUserId()).isEqualTo(1001L);
                    assertThat(dto.getRoleIds()).isEmpty();
                });
    }

    // TestCaseId: SYS-ADMIN-019
    @Test
    void removeAdminClearsSessionsRewritesUsernameDeletesProfileUserAndRoles() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(AdminBo.class));
        doReturn(true).when(service).removeById(10L);
        when(userService.removeUser(1001L)).thenReturn(true);

        service.removeAdmin(10L);

        verify(tokenSessionService).cleanupUserSessions(1001L);
        ArgumentCaptor<AdminBo> adminCaptor = ArgumentCaptor.forClass(AdminBo.class);
        verify(service).updateById(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getUsername()).isEqualTo("admin_del_10");
        verify(service).removeById(10L);
        verify(userService).removeUser(1001L);
        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(userRoleCaptor.getValue().getRoleIds()).isEmpty();
    }

    // TestCaseId: SYS-ADMIN-020
    @Test
    void updateAdminPartiallyUpdatesProfilePasswordAndReplacesRoles() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        existing.setNickname("旧昵称");
        existing.setAvatar("old.png");
        existing.setDescription("old desc");
        existing.setHomePath("/old");
        doReturn(existing).when(service).getById(10L);
        when(roleService.getRoleByCode("operator")).thenReturn(role("operator", 2L));
        doReturn(true).when(service).updateById(any(AdminBo.class));
        AdminDto dto = new AdminDto();
        dto.setId(10L);
        dto.setPassword("Newpass1!");
        dto.setNickname("新昵称");
        dto.setRoleCodes(List.of("operator"));

        service.updateAdmin(dto);

        ArgumentCaptor<AdminBo> adminCaptor = ArgumentCaptor.forClass(AdminBo.class);
        verify(service).updateById(adminCaptor.capture());
        AdminBo updated = adminCaptor.getValue();
        assertThat(updated.getUsername()).isEqualTo("admin");
        assertThat(passwordEncoder.matches("Newpass1!", updated.getPassword())).isTrue();
        assertThat(updated.getNickname()).isEqualTo("新昵称");
        assertThat(updated.getAvatar()).isEqualTo("old.png");
        assertThat(updated.getDescription()).isEqualTo("old desc");
        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(userRoleCaptor.getValue().getRoleIds()).containsExactly(2L);
    }

    // TestCaseId: SYS-ADMIN-021
    @Test
    void updateAdminLeavesRolesWhenRoleCodesAreNullAndClearsSessionsWhenDisabled() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        existing.setStatus(true);
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(AdminBo.class));
        AdminDto dto = new AdminDto();
        dto.setId(10L);
        dto.setStatus(false);

        service.updateAdmin(dto);

        verify(userRoleService, never()).updateUserRole(any(UserRoleDto.class));
        verify(tokenSessionService).cleanupUserSessions(1001L);
    }

    // TestCaseId: SYS-ADMIN-022
    @Test
    void updateAdminKeepsSessionsWhenAlreadyDisabled() {
        // Given：管理员在更新前已经处于禁用状态，本次只是修改昵称。
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        existing.setStatus(false);
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(AdminBo.class));
        AdminDto dto = new AdminDto();
        dto.setId(10L);
        dto.setNickname("禁用管理员");

        // When：调用 updateAdmin 方法。
        service.updateAdmin(dto);

        // Then：只有从启用变为禁用时才清理会话，已禁用资料更新不重复触发清理。
        verify(tokenSessionService, never()).cleanupUserSessions(any());
    }

    // TestCaseId: SYS-ADMIN-023
    @Test
    void updateAdminClearsRolesWhenRoleCodesAreEmpty() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(AdminBo.class));
        AdminDto dto = new AdminDto();
        dto.setId(10L);
        dto.setRoleCodes(List.of());

        service.updateAdmin(dto);

        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(userRoleCaptor.getValue().getRoleIds()).isEmpty();
        verify(tokenSessionService, never()).cleanupUserSessions(any());
    }

    // TestCaseId: SYS-ADMIN-024
    @Test
    void updateAdminRejectsWeakNewPasswordBeforePersistingProfile() {
        AdminBo existing = enabledAdmin();
        existing.setId(10L);
        doReturn(existing).when(service).getById(10L);
        AdminDto dto = new AdminDto();
        dto.setId(10L);
        dto.setPassword("weak");

        assertThatThrownBy(() -> service.updateAdmin(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("密码必须为8-32位且包含大写字母、小写字母、数字、特殊字符，不能包含空白字符");

        verify(service, never()).updateById(any(AdminBo.class));
        verify(userRoleService, never()).updateUserRole(any(UserRoleDto.class));
    }

    // TestCaseId: SYS-ADMIN-025
    @Test
    void createAdminCreatesUserProfileAndBindsValidatedRolesWithStrongPassword() {
        AdminDto dto = new AdminDto();
        dto.setUsername("admin");
        dto.setPassword("Strong1!");
        dto.setNickname("管理员");
        dto.setAvatar("avatar.png");
        dto.setDesc("desc");
        dto.setHomePath("/workspace");
        dto.setStatus(true);
        dto.setRoleCodes(List.of("admin", "operator"));
        RoleBo adminRole = role("admin", 1L);
        RoleBo operatorRole = role("operator", 2L);
        when(query.one()).thenReturn(null);
        when(userService.createUser()).thenReturn(1001L);
        when(roleService.getRoleByCode("admin")).thenReturn(adminRole);
        when(roleService.getRoleByCode("operator")).thenReturn(operatorRole);
        doReturn(true).when(service).save(any(AdminBo.class));

        service.createAdmin(dto);

        ArgumentCaptor<AdminBo> adminCaptor = ArgumentCaptor.forClass(AdminBo.class);
        verify(service).save(adminCaptor.capture());
        AdminBo saved = adminCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1001L);
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(passwordEncoder.matches("Strong1!", saved.getPassword())).isTrue();
        assertThat(saved.getDescription()).isEqualTo("desc");
        ArgumentCaptor<UserRoleDto> userRoleCaptor = ArgumentCaptor.forClass(UserRoleDto.class);
        verify(userRoleService).updateUserRole(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(userRoleCaptor.getValue().getRoleIds()).containsExactly(1L, 2L);
    }

    // TestCaseId: SYS-ADMIN-026
    @Test
    void createAdminRejectsWeakPasswordAsBusinessValidation() {
        AdminDto dto = new AdminDto();
        dto.setUsername("admin");
        dto.setPassword("weak");
        dto.setNickname("管理员");

        assertThatThrownBy(() -> service.createAdmin(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("密码必须为8-32位且包含大写字母、小写字母、数字、特殊字符，不能包含空白字符");

        verify(userService, never()).createUser();
        verify(service, never()).save(any(AdminBo.class));
    }

    // TestCaseId: SYS-ADMIN-027
    @Test
    void loginMatchesPasswordForEnabledAdminAndCreatesSession() {
        AdminBo admin = enabledAdmin();
        when(query.one()).thenReturn(admin);
        assertThat(passwordEncoder.matches("secret", admin.getPassword())).isTrue();
        when(tokenSessionService.createSession(1001L)).thenReturn(AdminSessionTokenPair.of("access", "refresh"));

        AdminTokenVo token = service.login("admin", "secret");

        assertThat(token.getAccessToken()).isEqualTo("access");
        assertThat(token.getRefreshToken()).isEqualTo("refresh");
        verify(tokenSessionService).createSession(1001L);
    }

    // TestCaseId: SYS-ADMIN-028
    @Test
    void loginReturnsNullForInvalidCredentialOrDisabledAdmin() {
        AdminBo admin = enabledAdmin();
        when(query.one()).thenReturn(admin);

        assertThat(service.login("admin", "bad")).isNull();
        verify(tokenSessionService, never()).createSession(any());

        admin.setStatus(false);
        assertThat(service.login("admin", "secret")).isNull();
    }

    // TestCaseId: SYS-ADMIN-029
    @Test
    void loginReturnsNullForUnknownUsernameWithoutCreatingSession() {
        // Given：mock 查询结果中没有匹配用户名的管理员记录。
        when(query.one()).thenReturn(null);

        // When：使用不存在的用户名调用 login 方法。
        AdminTokenVo token = service.login("missing", "secret");

        // Then：Service 返回认证失败结果，不抛出空指针，也不创建会话。
        assertThat(token).isNull();
        verify(tokenSessionService, never()).createSession(any());
    }

    // TestCaseId: SYS-ADMIN-030
    @Test
    void adminPageFiltersProfilesAndReturnsRoleCodesInDescendingIdOrder() {
        AdminPageDto dto = new AdminPageDto();
        dto.setUsername("admin");
        dto.setNickname("管");
        dto.setStatus(true);
        dto.setCurrentPage(1L);
        dto.setPageSize(2L);
        AdminBo newest = enabledAdmin();
        newest.setId(20L);
        newest.setNickname("管理员");
        newest.setAvatar("avatar.png");
        newest.setDescription("desc");
        newest.setHomePath("/workspace");
        AdminBo older = enabledAdmin();
        older.setId(10L);
        older.setUserId(1002L);
        older.setUsername("operator");
        IPage<AdminBo> page = new Page<>(1, 2, 2);
        page.setRecords(List.of(newest, older));
        when(query.page(any())).thenReturn(page);
        RoleBo adminRole = role("admin");
        RoleBo operatorRole = role("operator");
        when(roleService.getRoles(1001L)).thenReturn(List.of(adminRole, operatorRole));
        when(roleService.getRoles(1002L)).thenReturn(List.of(operatorRole));

        PageResult<AdminVo> result = service.getAdminPage(dto);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getList()).extracting(AdminVo::getId).containsExactly(20L, 10L);
        AdminVo first = result.getList().get(0);
        assertThat(first.getUsername()).isEqualTo("admin");
        assertThat(first.getDesc()).isEqualTo("desc");
        assertThat(first.getRoleCodes()).containsExactly("admin", "operator");
        verify(query).eq(ArgumentMatchers.eq(true), ArgumentMatchers.<SFunction<AdminBo, ?>>any(), ArgumentMatchers.eq("admin"));
        verify(query).like(ArgumentMatchers.eq(true), ArgumentMatchers.<SFunction<AdminBo, ?>>any(), ArgumentMatchers.eq("管"));
        verify(query).eq(ArgumentMatchers.eq(true), ArgumentMatchers.<SFunction<AdminBo, ?>>any(), ArgumentMatchers.eq(true));
        verify(query).orderByDesc(ArgumentMatchers.<SFunction<AdminBo, ?>>any());
    }

    // TestCaseId: SYS-ADMIN-031
    @Test
    void refreshAndLogoutDelegateToTokenSessionService() {
        when(tokenSessionService.refreshSession("refresh-token")).thenReturn(AdminSessionTokenPair.of("access", "refresh"));
        when(tokenSessionService.logout("refresh-token")).thenReturn(true);

        assertThat(service.refresh("refresh-token").getAccessToken()).isEqualTo("access");
        assertThat(service.logout("refresh-token")).isTrue();
    }

    // TestCaseId: SYS-ADMIN-032
    @Test
    void refreshReturnsNullWhenRefreshTokenIsBlankOrSessionRefreshFails() {
        assertThat(service.refresh(" ")).isNull();

        when(tokenSessionService.refreshSession("expired-refresh-token")).thenReturn(null);

        assertThat(service.refresh("expired-refresh-token")).isNull();
    }

    // TestCaseId: SYS-ADMIN-033
    @Test
    void userInfoReturnsProfileAndRoleCodesWithoutStatusOrTokens() {
        AdminBo admin = enabledAdmin();
        admin.setId(10L);
        admin.setNickname("管理员");
        admin.setAvatar("avatar.png");
        admin.setDescription("desc");
        admin.setHomePath("/workspace");
        when(query.one()).thenReturn(admin);
        bindPrincipal(1001L);
        RoleBo adminRole = role("admin");
        RoleBo blankRole = role(" ");
        RoleBo operatorRole = role("operator");
        when(roleService.getRoles(1001L)).thenReturn(List.of(adminRole, blankRole, operatorRole));

        AdminVo userInfo = service.getUserInfo();

        assertThat(userInfo.getId()).isEqualTo(10L);
        assertThat(userInfo.getUserId()).isEqualTo(1001L);
        assertThat(userInfo.getUsername()).isEqualTo("admin");
        assertThat(userInfo.getNickname()).isEqualTo("管理员");
        assertThat(userInfo.getRoleCodes()).containsExactly("admin", "operator");
        assertThat(userInfo.getStatus()).isNull();
        assertThat(userInfo).hasNoNullFieldsOrPropertiesExcept("avatar", "desc", "status", "createTime");
    }

    // TestCaseId: SYS-ADMIN-034
    @Test
    void menuAccessCodesReturnsDistinctNonblankEnabledButtonCodesOnly() {
        MenuBo create = menu(MenuType.BUTTON, true, "user:create");
        MenuBo duplicateCreate = menu(MenuType.BUTTON, true, "user:create");
        MenuBo disabled = menu(MenuType.BUTTON, false, "user:delete");
        MenuBo page = menu(MenuType.MENU, true, "user:list");
        MenuBo blank = menu(MenuType.BUTTON, true, " ");
        when(menuService.getMenus(1001L)).thenReturn(List.of(create, duplicateCreate, disabled, page, blank));

        List<String> accessCodes = service.getMenuAccessCodes(1001L);

        assertThat(accessCodes).containsExactly("user:create");
    }

    private AdminBo enabledAdmin() {
        AdminBo admin = new AdminBo();
        admin.setUserId(1001L);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("secret"));
        admin.setStatus(true);
        return admin;
    }

    private RoleBo role(String code) {
        return role(code, null);
    }

    private RoleBo role(String code, Long id) {
        RoleBo role = new RoleBo();
        role.setId(id);
        role.setCode(code);
        return role;
    }

    private AdminRoleDto adminRoleDto(Long adminId, List<String> roleCodes) {
        AdminRoleDto dto = new AdminRoleDto();
        dto.setId(adminId);
        dto.setRoleCodes(roleCodes);
        return dto;
    }

    private void bindPrincipal(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public java.security.Principal getUserPrincipal() {
                return DefaultAccessPrincipal.of(userId);
            }
        };
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private MenuBo menu(MenuType type, Boolean status, String accessCode) {
        MenuBo menu = new MenuBo();
        menu.setType(type);
        menu.setStatus(status);
        menu.setAccessCode(accessCode);
        return menu;
    }
}
