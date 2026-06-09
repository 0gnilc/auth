package com.gnilc.authz.rbac.provider.cache;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.TargetPermission;

import java.util.List;

/**
 * RBAC provider 使用的权限缓存。
 * <p>
 * 该接口只描述 provider 读取权限数据和重置缓存的能力，不限定具体缓存策略。
 */
public interface PermissionCache {
    /**
     * 加载目标权限集合。
     *
     * @return 目标权限列表
     */
    List<TargetPermission> loadTargetPermissions();

    /**
     * 重置目标权限缓存。
     */
    void resetTargetPermissions();

    /**
     * 加载指定用户拥有的权限集合。
     *
     * @param userId 用户 ID
     * @return 用户权限列表
     */
    List<Permission> loadUserPermissions(Long userId);

    /**
     * 重置指定用户的权限缓存。
     *
     * @param userId 用户 ID
     */
    void resetUserPermissions(Long userId);

    /**
     * 加载公开访问权限集合。
     *
     * @return 公开访问权限列表
     */
    List<Permission> loadPublicAccessPermissions();

    /**
     * 重置公开访问权限缓存。
     */
    void resetPublicAccessPermissions();

    /**
     * 重置所有 provider 权限缓存。
     */
    void resetAll();

}
