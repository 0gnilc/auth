package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import java.util.List;

/**
 * 默认权限缓存实现。
 * <p>
 * 该实现不保存本地状态，每次读取都委托给 {@link PermissionCacheLoader}。
 */
public class DefaultPermissionCache implements PermissionCache {
    /**
     * 权限数据加载器。
     */
    private final PermissionCacheLoader cacheLoader;

    /**
     * 创建默认权限缓存。
     *
     * @param cacheLoader 权限数据加载器
     */
    public DefaultPermissionCache(PermissionCacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    @Override
    public List<TargetPermission> loadTargetPermissions() {
        return cacheLoader.loadTargetPermissions();
    }

    @Override
    public void resetTargetPermissions() {
        // no local state
    }

    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        return cacheLoader.loadUserPermissions(userId);
    }

    @Override
    public void resetUserPermissions(Long userId) {
        // no local state
    }

    @Override
    public List<Permission> loadPublicAccessPermissions() {
        return cacheLoader.loadPublicAccessPermissions();
    }

    @Override
    public void resetPublicAccessPermissions() {
        // no local state
    }

    @Override
    public void resetAll() {
        // no local state
    }
}
