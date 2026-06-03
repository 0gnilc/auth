package com.gnilc.authz.rbac.cache;

import com.gnilc.authz.provider.Permission;

import java.util.List;

public interface PermissionCache {
    /**
     * 从缓存中获取所有资源的权限列表
     *
     * @return 权限列表
     */
    List<ResourcePermission> loadAllResourcePermissions();

    /**
     * 刷新缓存中所有资源的权限列表
     */
    void refreshAllResourcePermissions();

    /**
     * 根据用户id从缓存中获取用户拥有的权限列表
     *
     * @return 权限列表
     */
    List<Permission> loadUserPermissions(Long userId);

    /**
     * 根据用户id刷新缓存中用户拥有的权限列表
     */
    void refreshUserPermissions(Long userId);

    /**
     * 从缓存中获取所有公开的权限列表
     *
     * @return 权限列表
     */
    List<Permission> loadExposedPermissions();

    /**
     * 刷新缓存中所有公开的权限列表
     */
    void refreshExposedPermissions();

    /**
     * 清空所有缓存
     */
    void clear();
}
