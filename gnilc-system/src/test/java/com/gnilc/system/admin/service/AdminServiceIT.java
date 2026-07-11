package com.gnilc.system.admin.service;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.system.support.SystemContainerContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
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

    @AfterEach
    void cleanRedis() {
        try (RedisConnection connection = redis.getConnection()) {
            connection.serverCommands().flushDb();
        }
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

        AdminPageDto query = new AdminPageDto();
        query.setUsername("alice");
        assertThat(admins.getAdminPage(query).getList())
                .extracting(com.gnilc.system.admin.entity.vo.AdminVo::getUsername)
                .containsExactly("alice");

        admins.removeAdmin(stored.getId());
        assertThat(admins.getAdmin(stored.getId())).isNull();
        assertThat(admins.login("alice", "Strong#123")).isNull();
    }

    @Test
    void updateCanReplaceRolesChangePasswordAndDisableSessions() {
        ensureRole("reviewer");
        admins.createAdmin(admin("bob", "Initial#123", List.of()));
        AdminBo bob = admins.getAdminByUsername("bob");
        assertThat(admins.login("bob", "Initial#123")).isNotNull();

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
    }

    @Test
    void roleReplacementSupportsEmptyListAndRejectsUnknownRole() {
        admins.createAdmin(admin("carol", "Strong#123", null));
        AdminBo carol = admins.getAdminByUsername("carol");
        AdminRoleDto clear = new AdminRoleDto();
        clear.setId(carol.getId());
        clear.setRoleCodes(List.of());
        admins.updateAdminRoles(clear);
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
