package com.gnilc.system.admin.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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

    @AfterEach
    void cleanRedis() {
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
        assertThat(admins.getRoleCodes(stored.getUserId())).containsExactly("operator");

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
        assertThat(admins.getRoleCodes(bob.getUserId())).containsExactly("reviewer");
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
    void roleReplacementSupportsEmptyListAndRejectsUnknownRole() {
        ensureRole("operator");
        admins.createAdmin(admin("carol", "Strong#123", List.of("operator")));
        AdminBo carol = admins.getAdminByUsername("carol");

        AdminDto profileOnly = new AdminDto();
        profileOnly.setId(carol.getId());
        profileOnly.setNickname("Carol Updated");
        admins.updateAdmin(profileOnly);
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("operator");

        AdminRoleDto clear = new AdminRoleDto();
        clear.setId(carol.getId());
        clear.setRoleCodes(List.of());
        admins.updateAdminRoles(clear);
        assertThat(admins.getRoleCodes(carol.getUserId())).isEmpty();

        clear.setRoleCodes(List.of("operator"));
        admins.updateAdminRoles(clear);
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("operator");

        AdminDto clearThroughUpdate = new AdminDto();
        clearThroughUpdate.setId(carol.getId());
        clearThroughUpdate.setRoleCodes(List.of());
        admins.updateAdmin(clearThroughUpdate);
        assertThat(admins.getRoleCodes(carol.getUserId())).isEmpty();

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
        assertThat(page.get(1).getRoleCodes()).containsExactly("operator");
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
}
