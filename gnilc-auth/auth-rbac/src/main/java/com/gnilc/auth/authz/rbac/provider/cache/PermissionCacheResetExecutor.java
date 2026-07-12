package com.gnilc.auth.authz.rbac.provider.cache;

import org.springframework.stereotype.Component;

/**
 * 权限缓存重置命令执行器。
 * <p>
 * 将标准化重置命令映射为 {@link PermissionCache} 上的具体重置动作。
 */
@Component
public class PermissionCacheResetExecutor {
    /**
     * provider 权限缓存。
     */
    private final PermissionCache permissionCache;

    /**
     * 创建缓存重置执行器。
     *
     * @param permissionCache provider 权限缓存
     */
    public PermissionCacheResetExecutor(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    /**
     * 执行缓存重置命令。
     *
     * @param command 缓存重置命令
     */
    public void execute(PermissionCacheResetCommand command) {
        if (command == null || command.getTarget() == null) {
            return;
        }
        switch (command.getTarget()) {
            case TARGET_PERMISSIONS -> permissionCache.resetTargetPermissions();
            case PUBLIC_ACCESS_PERMISSIONS -> permissionCache.resetPublicAccessPermissions();
            case USER_PERMISSIONS -> {
                if (command.getUserId() != null) {
                    permissionCache.resetUserPermissions(command.getUserId());
                }
            }
            case ALL -> permissionCache.resetAll();
        }
    }
}
