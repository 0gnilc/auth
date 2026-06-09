package com.gnilc.authz.rbac.provider.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.bo.UserBo;
import com.gnilc.authz.rbac.provider.TargetPermission;
import com.gnilc.authz.rbac.service.PermissionService;
import com.gnilc.authz.rbac.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库权限缓存加载器。
 * <p>
 * 负责把 RBAC 持久化模型转换成 provider 可直接消费的权限读模型。
 */
@Component
public class DatabasePermissionCacheLoader implements PermissionCacheLoader {
    /**
     * 权限数据服务，用于读取目标权限、用户权限和公开访问权限。
     */
    @Autowired
    private PermissionService permissionService;
    /**
     * 用户数据服务，用于判断用户是否仍然存在。
     */
    @Autowired
    private UserServiceImpl userService;

    /**
     * 加载目标权限。
     *
     * @return 目标权限列表
     */
    @Override
    public List<TargetPermission> loadTargetPermissions() {
        return permissionService.list().stream().map(p ->
                new TargetPermission(p.getTargetIdentifier(), p.getCode())
        ).toList();
    }

    /**
     * 加载指定用户拥有的权限。
     *
     * @param userId 用户 ID
     * @return 用户权限列表
     */
    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        UserBo user = userService.geUser(userId);
        if (user == null) {
            return List.of();
        }
        return permissionService.getPermissions(userId)
                .stream().map(p -> new Permission(p.getCode()))
                .toList();
    }

    /**
     * 加载公开访问权限。
     *
     * @return 公开访问权限列表
     */
    @Override
    public List<Permission> loadPublicAccessPermissions() {
        return permissionService.list(new LambdaQueryWrapper<PermissionBo>()
                        .eq(PermissionBo::getPublicAccess, Boolean.TRUE))
                .stream()
                .map(p -> new Permission(p.getCode()))
                .toList();
    }
}
