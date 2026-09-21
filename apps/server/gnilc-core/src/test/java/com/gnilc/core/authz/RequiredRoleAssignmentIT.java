package com.gnilc.core.authz;

import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.core.support.SystemContainerContextInitializer;
import com.gnilc.core.support.SystemTestApplication;
import com.gnilc.core.admin.support.AdminApiTestConfiguration;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 从真实基线关联取得 RBAC 用户与角色，验证单独解绑和整组替换都不能移除身份必需角色。 */
@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Import(AdminApiTestConfiguration.class)
@Transactional
class RequiredRoleAssignmentIT {
    @Autowired private BaselineDataSeeder baseline;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRoleService userRoles;

    @BeforeEach
    void restoreBaseline() {
        baseline.seed();
    }

    /** 使用管理员基线身份验证同一通用关系入口执行应用策略；空角色集合也不能绕过保护。 */
    @Test
    void roleAssignmentCannotRemoveAdminBaselineRoles() {
        Long adminUserId = jdbc.queryForObject(
                "select user_id from sys_admin where username = 'admin' and del = 0", Long.class);
        Long adminRoleId = roleId("admin");

        assertThatThrownBy(() -> userRoles.unbindRole(adminUserId, adminRoleId))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");

        UserRoleDto assignment = new UserRoleDto();
        assignment.setUserId(adminUserId);
        assignment.setRoleIds(List.of());
        assertThatThrownBy(() -> userRoles.updateUserRole(assignment))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");
    }

    private Long roleId(String code) {
        return jdbc.queryForObject(
                "select id from az_role where code = ? and del = 0", Long.class, code);
    }
}
