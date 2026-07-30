package com.gnilc.auth.authz.rbac.provider.cache;

import org.springframework.stereotype.Component;

/**
 * 权限缓存重置命令执行器。
 * <p>
 * 将标准化重置命令映射为 {@link PermissionCacheService} 上的具体重置动作。
 */
@Component
public class PermissionCacheResetExecutor {
    /**
     * provider 权限缓存。
     */
    private final PermissionCacheService cacheService;

    /**
     * 创建缓存重置执行器。
     *
     * @param cacheService provider 权限缓存服务
     */
    public PermissionCacheResetExecutor(PermissionCacheService cacheService) {
        this.cacheService = cacheService;
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
            case TARGET_PERMISSIONS -> cacheService.resetTargetPermissions();
            case PUBLIC_ACCESS_PERMISSIONS -> cacheService.resetPublicAccessPermissions();
            case USER_PERMISSIONS -> {
                if (command.getUserId() != null) {
                    cacheService.resetUserPermissions(command.getUserId());
                }
            }
            case ALL -> cacheService.resetAll();
        }
    }
}
