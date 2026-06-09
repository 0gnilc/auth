package com.gnilc.authz.rbac.provider.cache;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.TargetPermission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认权限缓存实现。
 * <p>
 * 该实现不保存本地状态，每次读取都委托给 {@link PermissionCacheLoader}。
 */
@Component
@ConditionalOnMissingBean(PermissionCache.class)
public class DefaultPermissionCache implements PermissionCache {
    /**
     * 权限数据加载器。
     */
    private final PermissionCacheLoader permissionCacheLoader;

    /**
     * 创建默认权限缓存。
     *
     * @param permissionCacheLoader 权限数据加载器
     */
    public DefaultPermissionCache(PermissionCacheLoader permissionCacheLoader) {
        this.permissionCacheLoader = permissionCacheLoader;
    }

    @Override
    public List<TargetPermission> loadTargetPermissions() {
        return permissionCacheLoader.loadTargetPermissions();
    }

    @Override
    public void resetTargetPermissions() {
        // no local state
    }

    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        return permissionCacheLoader.loadUserPermissions(userId);
    }

    @Override
    public void resetUserPermissions(Long userId) {
        // no local state
    }

    @Override
    public List<Permission> loadPublicAccessPermissions() {
        return permissionCacheLoader.loadPublicAccessPermissions();
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
