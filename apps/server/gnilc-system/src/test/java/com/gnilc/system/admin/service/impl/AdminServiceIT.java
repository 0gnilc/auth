package com.gnilc.system.admin.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.service.AdminService;
import com.gnilc.system.admin.entity.vo.AdminVo;
import com.gnilc.system.session.AdminSessionManager;
import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.test.cleanup.RedisCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class AdminServiceIT {
    @Autowired private AdminService admins;
    @Autowired private RoleService roles;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdminSessionManager sessions;

    @BeforeEach
    void cleanRedisBeforeTest() {
        cleanRedis();
        ensureRole("admin");
    }

    @AfterEach
    void cleanRedisAfterTest() {
        RequestContextHolder.resetRequestAttributes();
        cleanRedis();
    }

    private void cleanRedis() {
        new RedisCleaner(redis).flushDatabase();
    }

    @Test
    void createLoginPageAndRemoveAdminRemainConsistentWithRbacUser() {
        ensureRole("operator");
        AdminDto create = admin("alice", "Strong#123", List.of("operator"));
        admins.createAdmin(create);
        AdminBo stored = admins.getAdminByUsername("alice");

        assertThat(stored.getUserId()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("Strong#123", stored.getPassword())).isTrue();
        assertThat(admins.getRoleCodes(stored.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminTokenVo token = admins.login("alice", "Strong#123");
        assertThat(token).isNotNull();
        assertThat(token.getAccessToken()).startsWith("sys_admin." + stored.getUserId() + ".");
        admins.createAdmin(admin("other-admin", "Strong#123", null));
        AdminTokenVo otherToken = admins.login("other-admin", "Strong#123");

        AdminPageDto query = new AdminPageDto();
        query.setUsername("alice");
        assertThat(admins.getAdminPage(query).getList())
                .extracting(com.gnilc.system.admin.entity.vo.AdminVo::getUsername)
                .containsExactly("alice");

        admins.removeAdmin(stored.getId());
        assertThat(admins.getAdmin(stored.getId())).isNull();
        assertThat(admins.login("alice", "Strong#123")).isNull();
        assertThat(sessions.validateAccessToken(token.getAccessToken())).isNull();
        assertThat(admins.refresh(token.getRefreshToken())).isNull();
        assertThat(sessions.validateAccessToken(otherToken.getAccessToken()))
                .isEqualTo(admins.getAdminByUsername("other-admin").getUserId());
        assertThat(admins.refresh(otherToken.getRefreshToken())).isNotNull();
        assertThat(jdbc.queryForObject(
                "select username from sys_admin where id = ?", String.class, stored.getId()))
                .isEqualTo("alice_del_" + stored.getId());
        assertThat(jdbc.queryForObject(
                "select del from az_user where id = ?", Integer.class, stored.getUserId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*)
                  from az_user_role
                 where user_id = ? and del = 0
                """, Integer.class, stored.getUserId())).isZero();

        admins.createAdmin(admin("alice", "Replacement#123", List.of("operator")));
        assertThat(admins.getAdminByUsername("alice").getId()).isNotEqualTo(stored.getId());
    }

    @Test
    void updateCanReplaceRolesChangePasswordAndDisableSessions() {
        ensureRole("reviewer");
        admins.createAdmin(admin("bob", "Initial#123", List.of()));
        AdminBo bob = admins.getAdminByUsername("bob");
        AdminTokenVo firstBobSession = admins.login("bob", "Initial#123");
        AdminTokenVo secondBobSession = admins.login("bob", "Initial#123");
        admins.createAdmin(admin("still-enabled", "Initial#123", List.of()));
        AdminTokenVo enabledSession = admins.login("still-enabled", "Initial#123");

        AdminDto update = new AdminDto();
        update.setId(bob.getId());
        update.setPassword("Changed#456");
        update.setNickname("Robert");
        update.setStatus(false);
        update.setRoleCodes(List.of("reviewer"));
        admins.updateAdmin(update);

        assertThat(admins.getAdmin(bob.getId()).getNickname()).isEqualTo("Robert");
        assertThat(admins.getRoleCodes(bob.getUserId()))
                .containsExactlyInAnyOrder("admin", "reviewer");
        assertThat(admins.login("bob", "Changed#456")).isNull();
        assertThat(sessions.validateAccessToken(firstBobSession.getAccessToken())).isNull();
        assertThat(sessions.validateAccessToken(secondBobSession.getAccessToken())).isNull();
        assertThat(admins.refresh(firstBobSession.getRefreshToken())).isNull();
        assertThat(admins.refresh(secondBobSession.getRefreshToken())).isNull();
        assertThat(sessions.validateAccessToken(enabledSession.getAccessToken()))
                .isEqualTo(admins.getAdminByUsername("still-enabled").getUserId());
        assertThat(admins.refresh(enabledSession.getRefreshToken())).isNotNull();
    }

    @Test
    void updateKeepsPasswordWhenPasswordIsBlankOrNull() {
        admins.createAdmin(admin("password-kept", "Initial#123", List.of()));
        AdminBo stored = admins.getAdminByUsername("password-kept");

        AdminDto blankPassword = new AdminDto();
        blankPassword.setId(stored.getId());
        blankPassword.setPassword("  ");
        admins.updateAdmin(blankPassword);
        assertThat(admins.login("password-kept", "Initial#123")).isNotNull();

        AdminDto nullPassword = new AdminDto();
        nullPassword.setId(stored.getId());
        nullPassword.setPassword(null);
        admins.updateAdmin(nullPassword);
        assertThat(admins.login("password-kept", "Initial#123")).isNotNull();
    }

    @Test
    void roleReplacementSupportsEmptyListAndRejectsUnknownRole() {
        ensureRole("operator");
        admins.createAdmin(admin("carol", "Strong#123", List.of("operator")));
        AdminBo carol = admins.getAdminByUsername("carol");

        AdminDto profileOnly = new AdminDto();
        profileOnly.setId(carol.getId());
        profileOnly.setNickname("Carol Updated");
        admins.updateAdmin(profileOnly);
        assertThat(admins.getRoleCodes(carol.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminRoleDto clear = new AdminRoleDto();
        clear.setId(carol.getId());
        clear.setRoleCodes(List.of());
        admins.updateAdminRoles(clear);
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("admin");

        clear.setRoleCodes(List.of("operator"));
        admins.updateAdminRoles(clear);
        assertThat(admins.getRoleCodes(carol.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminDto clearThroughUpdate = new AdminDto();
        clearThroughUpdate.setId(carol.getId());
        clearThroughUpdate.setRoleCodes(List.of());
        admins.updateAdmin(clearThroughUpdate);
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("admin");

        clear.setRoleCodes(List.of("missing"));
        assertThatThrownBy(() -> admins.updateAdminRoles(clear))
                .isInstanceOf(IllegalConditionException.class);
    }

    @Test
    void createRejectsWeakPasswordsAndDuplicateUsernames() {
        assertThatThrownBy(() -> admins.createAdmin(admin("weak", "password", null)))
                .isInstanceOf(InvalidArgumentException.class);

        admins.createAdmin(admin("unique", "Strong#123", null));
        assertThatThrownBy(() -> admins.createAdmin(admin("unique", "Another#123", null)))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void createDefaultsBlankHomePathToDashboard() {
        AdminDto missing = admin("home-missing", "Strong#123", List.of());
        missing.setHomePath(null);
        admins.createAdmin(missing);
        assertThat(admins.getAdminByUsername("home-missing").getHomePath())
                .isEqualTo("/dashboard");

        AdminDto blank = admin("home-blank", "Strong#123", List.of());
        blank.setHomePath("  ");
        admins.createAdmin(blank);
        assertThat(admins.getAdminByUsername("home-blank").getHomePath())
                .isEqualTo("/dashboard");
    }

    @Test
    void currentProfileUpdateUsesPrincipalUserIdAndOnlyChangesEditableFields() {
        ensureRole("operator");
        AdminDto current = admin("profile-owner", "Initial#123", List.of("operator"));
        current.setAvatar("https://example.test/old.png");
        current.setDesc("Old description");
        admins.createAdmin(current);
        AdminBo owner = admins.getAdminByUsername("profile-owner");

        admins.createAdmin(admin("other-profile", "Other#123", List.of()));
        AdminBo other = admins.getAdminByUsername("other-profile");
        authenticateAs(owner.getUserId());

        AdminDto update = new AdminDto();
        update.setId(other.getId());
        update.setUsername("hijacked");
        update.setPassword("Changed#456");
        update.setNickname("Updated Owner");
        update.setAvatar("  ");
        update.setDesc(" ");
        update.setHomePath("/hijacked");
        update.setStatus(false);
        update.setRoleCodes(List.of());
        admins.updateProfile(update);

        AdminBo updated = admins.getAdmin(owner.getId());
        assertThat(updated.getUsername()).isEqualTo("profile-owner");
        assertThat(updated.getNickname()).isEqualTo("Updated Owner");
        assertThat(updated.getAvatar()).isNull();
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getHomePath()).isEqualTo("/workspace");
        assertThat(updated.getStatus()).isTrue();
        assertThat(admins.getRoleCodes(owner.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");
        assertThat(admins.login("profile-owner", "Initial#123")).isNotNull();
        assertThat(admins.getAdmin(other.getId()).getNickname()).isEqualTo("other-profile");
    }

    @Test
    void currentProfileUpdateRejectsBlankNicknameWithoutChangingProfile() {
        admins.createAdmin(admin("invalid-profile", "Initial#123", List.of()));
        AdminBo owner = admins.getAdminByUsername("invalid-profile");
        authenticateAs(owner.getUserId());

        AdminDto update = new AdminDto();
        update.setNickname("   ");
        update.setAvatar("https://example.test/changed.png");

        assertThatThrownBy(() -> admins.updateProfile(update))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Nickname is required.");
        assertThat(admins.getAdmin(owner.getId()).getNickname()).isEqualTo("invalid-profile");
        assertThat(admins.getAdmin(owner.getId()).getAvatar()).isNull();
    }

    @Test
    void currentPasswordUpdateChangesPasswordAndRevokesOnlyCurrentUsersSessions() {
        admins.createAdmin(admin("password-owner", "Initial#123", List.of()));
        AdminBo owner = admins.getAdminByUsername("password-owner");
        AdminTokenVo firstSession = admins.login("password-owner", "Initial#123");
        AdminTokenVo secondSession = admins.login("password-owner", "Initial#123");

        admins.createAdmin(admin("password-other", "Other#123", List.of()));
        AdminTokenVo otherSession = admins.login("password-other", "Other#123");
        AdminBo other = admins.getAdminByUsername("password-other");
        authenticateAs(owner.getUserId());

        admins.updatePassword("Initial#123", "Changed#456");

        assertThat(admins.login("password-owner", "Initial#123")).isNull();
        assertThat(admins.login("password-owner", "Changed#456")).isNotNull();
        assertThat(sessions.validateAccessToken(firstSession.getAccessToken())).isNull();
        assertThat(sessions.validateAccessToken(secondSession.getAccessToken())).isNull();
        assertThat(admins.refresh(firstSession.getRefreshToken())).isNull();
        assertThat(admins.refresh(secondSession.getRefreshToken())).isNull();
        assertThat(sessions.validateAccessToken(otherSession.getAccessToken()))
                .isEqualTo(other.getUserId());
        assertThat(admins.refresh(otherSession.getRefreshToken())).isNotNull();
    }

    @Test
    void currentPasswordUpdateRejectsInvalidCredentialsWithoutChangingPasswordOrSessions() {
        admins.createAdmin(admin("password-invalid", "Initial#123", List.of()));
        AdminBo owner = admins.getAdminByUsername("password-invalid");
        AdminTokenVo session = admins.login("password-invalid", "Initial#123");
        authenticateAs(owner.getUserId());

        assertThatThrownBy(() -> admins.updatePassword("Wrong#123", "Changed#456"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Current password is incorrect.");
        assertThatThrownBy(() -> admins.updatePassword("Initial#123", "weak"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("Password must be 8 to 32 characters");

        assertThat(admins.login("password-invalid", "Initial#123")).isNotNull();
        assertThat(admins.login("password-invalid", "Changed#456")).isNull();
        assertThat(sessions.validateAccessToken(session.getAccessToken()))
                .isEqualTo(owner.getUserId());
        assertThat(admins.refresh(session.getRefreshToken())).isNotNull();
    }

    @Test
    void pageFiltersOrdersProfilesAndDeduplicatesRoleCodes() {
        ensureRole("operator");
        AdminDto first = admin("page-one", "Strong#123", List.of("operator"));
        first.setNickname("Team Alpha");
        first.setAvatar("https://example.test/alpha.png");
        first.setDesc("First operator");
        admins.createAdmin(first);
        AdminBo firstStored = admins.getAdminByUsername("page-one");

        AdminDto second = admin("page-two", "Strong#123", List.of("operator"));
        second.setNickname("Team Beta");
        admins.createAdmin(second);
        AdminDto disabled = admin("page-disabled", "Strong#123", List.of("operator"));
        disabled.setNickname("Team Disabled");
        disabled.setStatus(false);
        admins.createAdmin(disabled);

        Long operatorRoleId = roles.getRoleByCode("operator").getId();
        jdbc.update("""
                insert into az_user_role (del, create_time, user_id, role_id)
                values (0, now(), ?, ?)
                """, firstStored.getUserId(), operatorRoleId);

        AdminPageDto query = new AdminPageDto();
        query.setNickname("Team");
        query.setStatus(true);
        List<AdminVo> page = admins.getAdminPage(query).getList();

        assertThat(page).extracting(AdminVo::getUsername)
                .containsExactly("page-two", "page-one");
        assertThat(page.get(1).getAvatar()).isEqualTo("https://example.test/alpha.png");
        assertThat(page.get(1).getDesc()).isEqualTo("First operator");
        assertThat(page.get(1).getHomePath()).isEqualTo("/workspace");
        assertThat(page.get(1).getStatus()).isTrue();
        assertThat(page.get(1).getRoleCodes())
                .containsExactlyInAnyOrder("admin", "operator");
    }

    private AdminDto admin(String username, String password, List<String> roleCodes) {
        AdminDto dto = new AdminDto();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setNickname(username);
        dto.setHomePath("/workspace");
        dto.setStatus(true);
        dto.setRoleCodes(roleCodes);
        return dto;
    }

    private void ensureRole(String code) {
        if (roles.getRoleByCode(code) != null) {
            return;
        }
        RoleDto dto = new RoleDto();
        dto.setCode(code);
        dto.setName(code);
        roles.createRole(dto);
    }

    private void authenticateAs(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(userId));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
