package com.gnilc.core.authz;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.service.RequiredRolePolicy;
import com.gnilc.core.admin.dao.AdminDao;
import com.gnilc.core.admin.entity.bo.AdminBo;
import org.springframework.stereotype.Component;

/**
 * 声明管理员身份必须保留的基础角色。
 */
@Component
public class AdminRequiredRolePolicy implements RequiredRolePolicy {
    private static final String ADMIN_ROLE = "admin";

    private final AdminDao adminDao;
    private final RoleDao roleDao;

    public AdminRequiredRolePolicy(AdminDao adminDao, RoleDao roleDao) {
        this.adminDao = adminDao;
        this.roleDao = roleDao;
    }

    /** 按现存管理员身份保护基础角色；不在此授予角色，也不以账户启用状态决定是否允许移除。 */
    @Override
    public boolean isRequired(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        RoleBo role = roleDao.selectById(roleId);
        if (role == null) {
            return false;
        }
        // 身份约束留在应用层，通用角色关系只调用策略；普通管理角色不因此成为不可移除角色。
        return switch (role.getCode()) {
            case ADMIN_ROLE -> hasAdmin(userId);
            default -> false;
        };
    }

    private boolean hasAdmin(Long userId) {
        return adminDao.selectCount(Wrappers.<AdminBo>lambdaQuery()
                .eq(AdminBo::getUserId, userId)) > 0;
    }

}
