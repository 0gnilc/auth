package com.gnilc.authz.rbac.provider.cache;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.TargetPermission;

import java.util.List;

/**
 * 权限缓存数据加载器。
 * <p>
 * 负责从真实数据源加载 provider 所需的权限数据，缓存实现只负责复用这些加载结果。
 */
public interface PermissionCacheLoader {
    /**
     * 加载目标权限集合。
     *
     * @return 目标权限列表
     */
    List<TargetPermission> loadTargetPermissions();

    /**
     * 加载指定用户拥有的权限集合。
     *
     * @param userId 用户 ID
     * @return 用户权限列表
     */
    List<Permission> loadUserPermissions(Long userId);

    /**
     * 加载公开访问权限集合。
     *
     * @return 公开访问权限列表
     */
    List<Permission> loadPublicAccessPermissions();
}
