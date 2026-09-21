package com.gnilc.core.authz;

import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.core.admin.dao.AdminDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 隔离身份和角色查询，验证基础角色只受对应身份保护，其他角色或不存在的身份不被误判为必需。 */
class AdminRequiredRolePolicyTest {
    private final AdminDao admins = mock(AdminDao.class);
    private final RoleDao roles = mock(RoleDao.class);
    private final AdminRequiredRolePolicy policy =
            new AdminRequiredRolePolicy(admins, roles);

    @Test
    void adminBaselineRoleIsRequiredForItsIdentity() {
        when(roles.selectById(11L)).thenReturn(role("admin"));
        when(admins.selectCount(any())).thenReturn(1L);

        assertThat(policy.isRequired(7L, 11L)).isTrue();
    }

    @Test
    void otherRolesAndUnrelatedIdentitiesAreNotRequired() {
        when(roles.selectById(11L)).thenReturn(role("admin"));
        when(roles.selectById(13L)).thenReturn(role("manager"));

        assertThat(policy.isRequired(7L, 11L)).isFalse();
        assertThat(policy.isRequired(7L, 13L)).isFalse();
        assertThat(policy.isRequired(7L, 99L)).isFalse();
    }

    private RoleBo role(String code) {
        RoleBo role = new RoleBo();
        role.setCode(code);
        return role;
    }
}
