package com.gnilc.system.admin.service;

import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.auth.authz.rbac.service.impl.RoleServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserRoleServiceImpl;
import com.gnilc.system.admin.dao.AdminDao;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.service.impl.AdminServiceImpl;
import com.gnilc.system.session.AdminSessionManager;
import com.gnilc.system.session.AdminSessionTokenPair;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.test.container.MySqlContainerContextInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationTest
@SpringBootTest(
        classes = AdminServiceIT.ServiceTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        }
)
@ContextConfiguration(initializers = MySqlContainerContextInitializer.class)
@Transactional
class AdminServiceIT {
    @Autowired
    private AdminService adminService;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private com.gnilc.auth.authz.rbac.dao.UserDao userDao;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private AdminSessionManager sessionManager;
    @MockBean
    private MenuService menuService;

    @BeforeEach
    void createRoles() {
        saveRole("operator", "Operator");
        saveRole("auditor", "Auditor");
    }

    @Test
    void createsARealUserAdminProfileAndRoleBinding() {
        AdminDto dto = admin("service-admin", "ServiceAdmin1!", List.of("operator"));

        adminService.createAdmin(dto);

        AdminBo stored = adminService.getAdminByUsername("service-admin");
        assertThat(stored).isNotNull();
        assertThat(stored.getUserId()).isNotNull();
        assertThat(stored.getPassword()).isNotEqualTo("ServiceAdmin1!");
        assertThat(stored.getNickname()).isEqualTo("Service Administrator");
        assertThat(userDao.selectById(stored.getUserId())).isNotNull();
        assertThat(adminService.getRoleCodes(stored.getUserId())).containsExactly("operator");
    }

    @Test
    void loginUsesThePersistedPasswordAndCreatesARealSessionContract() {
        adminService.createAdmin(admin("login-admin", "LoginAdmin1!", List.of()));
        AdminBo stored = adminService.getAdminByUsername("login-admin");
        when(sessionManager.createSession(stored.getUserId()))
                .thenReturn(AdminSessionTokenPair.of("access-token", "refresh-token"));

        AdminTokenVo token = adminService.login("login-admin", "LoginAdmin1!");

        assertThat(token.getAccessToken()).isEqualTo("access-token");
        assertThat(token.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(adminService.login("login-admin", "wrong-password")).isNull();
        verify(sessionManager).createSession(stored.getUserId());
    }

    @Test
    void updatesProfileAndReplacesItsPersistedRoleSet() {
        adminService.createAdmin(admin("updated-admin", "UpdatedAdmin1!", List.of("operator")));
        AdminBo stored = adminService.getAdminByUsername("updated-admin");
        AdminDto update = new AdminDto();
        update.setId(stored.getId());
        update.setNickname("Updated Nickname");
        update.setRoleCodes(List.of("auditor"));

        adminService.updateAdmin(update);

        AdminBo updated = adminService.getAdmin(stored.getId());
        assertThat(updated.getNickname()).isEqualTo("Updated Nickname");
        assertThat(adminService.getRoleCodes(updated.getUserId())).containsExactly("auditor");
    }

    @Test
    void disablingAnEnabledAdministratorClearsSessionsButAProfileEditDoesNot() {
        adminService.createAdmin(admin("disabled-admin", "DisabledAdmin1!", List.of()));
        AdminBo stored = adminService.getAdminByUsername("disabled-admin");
        AdminDto nicknameOnly = new AdminDto();
        nicknameOnly.setId(stored.getId());
        nicknameOnly.setNickname("Still Enabled");
        adminService.updateAdmin(nicknameOnly);
        verify(sessionManager, never()).cleanupUserSessions(stored.getUserId());

        AdminDto disable = new AdminDto();
        disable.setId(stored.getId());
        disable.setStatus(false);
        adminService.updateAdmin(disable);

        verify(sessionManager).cleanupUserSessions(stored.getUserId());
        assertThat(adminService.login("disabled-admin", "DisabledAdmin1!")).isNull();
    }

    @Test
    void failedCreateRollsBackRealUserAdminAndRoleWrites() {
        long usersBefore = userDao.selectCount(null);
        long adminsBefore = adminDao.selectCount(null);
        AdminDto invalid = admin("rollback-admin", "RollbackAdmin1!", List.of("operator", "missing-role"));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> adminService.createAdmin(invalid)))
                .isInstanceOf(RuntimeException.class);

        assertThat(adminService.getAdminByUsername("rollback-admin")).isNull();
        assertThat(adminDao.selectCount(null)).isEqualTo(adminsBefore);
        assertThat(userDao.selectCount(null)).isEqualTo(usersBefore);
    }

    @Test
    void renamedRoleCodeIsUsedForLookupAndCanStillBeCleared() {
        adminService.createAdmin(admin("renamed-role-admin", "RenamedRole1!", List.of("operator")));
        AdminBo stored = adminService.getAdminByUsername("renamed-role-admin");
        RoleBo operator = roleService.getRoleByCode("operator");
        RoleDto rename = new RoleDto();
        rename.setId(operator.getId());
        rename.setCode("support-operator");
        rename.setName("Support Operator");
        roleService.updateRole(rename);

        assertThat(adminService.getRoleCodes(stored.getUserId())).containsExactly("support-operator");
        AdminRoleDto clear = new AdminRoleDto();
        clear.setId(stored.getId());
        clear.setRoleCodes(List.of());
        adminService.updateAdminRoles(clear);

        assertThat(adminService.getRoleCodes(stored.getUserId())).isEmpty();
        assertThat(userRoleService.getRoleIds(stored.getUserId())).isEmpty();
    }

    @Test
    void removingAnAdministratorRevokesSessionsAndLogicallyDeletesAdminUserAndRoles() {
        adminService.createAdmin(admin("removed-admin", "RemovedAdmin1!", List.of("operator")));
        AdminBo stored = adminService.getAdminByUsername("removed-admin");
        Long adminId = stored.getId();
        Long userId = stored.getUserId();

        adminService.removeAdmin(adminId);

        verify(sessionManager).cleanupUserSessions(userId);
        assertThat(adminService.getAdmin(adminId)).isNull();
        assertThat(adminService.getAdminByUsername("removed-admin")).isNull();
        assertThat(userDao.selectById(userId)).isNull();
        assertThat(userRoleService.getRoleIds(userId)).isEmpty();
    }

    @Test
    void explicitRoleUpdateCanClearAllPersistedBindings() {
        adminService.createAdmin(admin("roles-admin", "RolesAdmin1!", List.of("operator", "auditor")));
        AdminBo stored = adminService.getAdminByUsername("roles-admin");
        AdminRoleDto update = new AdminRoleDto();
        update.setId(stored.getId());
        update.setRoleCodes(List.of());

        adminService.updateAdminRoles(update);

        assertThat(adminService.getRoleCodes(stored.getUserId())).isEmpty();
    }

    private AdminDto admin(String username, String password, List<String> roleCodes) {
        AdminDto dto = new AdminDto();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setNickname("Service Administrator");
        dto.setHomePath("/workspace");
        dto.setStatus(true);
        dto.setRoleCodes(roleCodes);
        return dto;
    }

    private void saveRole(String code, String name) {
        if (roleService.getRoleByCode(code) != null) {
            return;
        }
        RoleBo role = new RoleBo();
        role.setCode(code);
        role.setName(name);
        role.setBuiltIn(false);
        roleService.save(role);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AdminServiceImpl.class,
            RoleServiceImpl.class,
            UserRoleServiceImpl.class,
            TestUserService.class,
            MyMetaObjectHandler.class
    })
    @MapperScan(basePackageClasses = {
            AdminDao.class,
            com.gnilc.auth.authz.rbac.dao.RoleDao.class,
            com.gnilc.auth.authz.rbac.dao.UserDao.class,
            com.gnilc.auth.authz.rbac.dao.UserRoleDao.class
    })
    static class ServiceTestApplication {
    }

    static class TestUserService extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<
            com.gnilc.auth.authz.rbac.dao.UserDao, UserBo> implements UserService {
        @Override
        public Long createUser() {
            UserBo user = new UserBo();
            save(user);
            return user.getId();
        }

        @Override
        public boolean removeUser(Long userId) {
            removeById(userId);
            return true;
        }

        @Override
        public boolean bindRole(Long userId, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unbindRole(Long userId, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RoleBo> getRoles(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkRole(Long userId, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.gnilc.auth.authz.rbac.entity.bo.PermissionBo> getPermissions(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.gnilc.auth.authz.rbac.entity.bo.MenuBo> getMenus(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserBo geUser(Long userId) {
            return getById(userId);
        }
    }
}
