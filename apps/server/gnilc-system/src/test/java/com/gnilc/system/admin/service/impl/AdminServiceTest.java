package com.gnilc.system.admin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.system.admin.dao.AdminDao;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.session.AdminSessionManager;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    private static final long ADMIN_ID = 41L;
    private static final long USER_ID = 84L;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private AdminDao adminDao;
    @Mock
    private AdminSessionManager sessions;
    @Mock
    private RoleService roles;
    @Mock
    private MenuService menus;
    @Mock
    private UserService users;
    @Mock
    private UserRoleService userRoles;

    private AdminServiceImpl admins;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(AdminBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "admin-service-test"),
                    AdminBo.class);
        }
        admins = new AdminServiceImpl(sessions, roles, menus, users, userRoles);
        ReflectionTestUtils.setField(admins, "baseMapper", adminDao);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(USER_ID));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void updateUserInfoUsesCurrentUserAndOnlyWritesEditableFields() {
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());
        when(adminDao.update(isNull(), any())).thenReturn(1);
        AdminDto profile = new AdminDto();
        profile.setId(999L);
        profile.setUsername("other");
        profile.setNickname("  Updated Admin  ");
        profile.setAvatar("  ");
        profile.setDesc(" ");
        profile.setHomePath("/other");
        profile.setStatus(false);

        admins.updateUserInfo(profile);

        ArgumentCaptor<Wrapper<AdminBo>> update = wrapperCaptor();
        verify(adminDao).update(isNull(), update.capture());
        Wrapper<AdminBo> wrapper = update.getValue();
        LambdaUpdateWrapper<AdminBo> lambdaUpdate = asLambdaUpdate(wrapper);
        assertThat(wrapper.getSqlSet())
                .contains("nickname", "avatar", "description")
                .doesNotContain("username", "home_path", "status");
        assertThat(wrapper.getSqlSegment()).contains("id");
        assertThat(lambdaUpdate.getParamNameValuePairs())
                .containsValue("Updated Admin")
                .containsValue(ADMIN_ID);
    }

    @Test
    void updateUserInfoRejectsOversizedProfileFieldsBeforeWriting() {
        AdminDto profile = new AdminDto();
        profile.setNickname("n".repeat(256));

        assertThatThrownBy(() -> admins.updateUserInfo(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Nickname must be at most 255 characters.");

        profile.setNickname("Admin");
        profile.setAvatar("a".repeat(501));
        assertThatThrownBy(() -> admins.updateUserInfo(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Avatar URL must be at most 500 characters.");

        profile.setAvatar(null);
        profile.setDesc("d".repeat(501));
        assertThatThrownBy(() -> admins.updateUserInfo(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Description must be at most 500 characters.");
        verify(adminDao, never()).update(isNull(), any());
    }

    @Test
    void updatePasswordEncodesPasswordAndRevokesAllCurrentUserSessions() {
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());
        when(adminDao.update(isNull(), any())).thenReturn(1);

        admins.updatePassword("Initial#123", "Changed#456");

        ArgumentCaptor<Wrapper<AdminBo>> update = wrapperCaptor();
        verify(adminDao).update(isNull(), update.capture());
        assertThat(asLambdaUpdate(update.getValue()).getParamNameValuePairs().values())
                .anyMatch(value -> value instanceof String passwordHash
                        && PASSWORD_ENCODER.matches("Changed#456", passwordHash));
        verify(sessions).cleanupUserSessions(USER_ID);
    }

    @Test
    void updatePasswordRejectsWrongCurrentPasswordWithoutWritingOrRevokingSessions() {
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());

        assertThatThrownBy(() -> admins.updatePassword("Wrong#123", "Changed#456"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Current password is incorrect.");
        verify(adminDao, never()).update(isNull(), any());
        verify(sessions, never()).cleanupUserSessions(any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Wrapper<AdminBo>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<AdminBo> asLambdaUpdate(Wrapper<AdminBo> wrapper) {
        assertThat(wrapper).isInstanceOf(LambdaUpdateWrapper.class);
        return (LambdaUpdateWrapper<AdminBo>) wrapper;
    }

    private AdminBo currentAdmin() {
        AdminBo admin = new AdminBo();
        admin.setId(ADMIN_ID);
        admin.setUserId(USER_ID);
        admin.setPassword(PASSWORD_ENCODER.encode("Initial#123"));
        return admin;
    }
}
