package com.gnilc.auth.authz.rbac.provider.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 标准化权限缓存重置命令。
 * <p>
 * Spring 事件和 Redis 消息都传递该命令，避免在多个节点重复推导业务事件影响范围。
 */
@Setter
@Getter
public class PermissionCacheResetCommand {
    /**
     * 需要重置的缓存目标。
     */
    private Target target;
    /**
     * 用户权限缓存目标对应的用户 ID，其他目标不需要该字段。
     */
    private Long userId;

    public PermissionCacheResetCommand() {
    }

    /**
     * 创建缓存重置命令。
     *
     * @param target 重置目标
     * @param userId 用户 ID，仅 {@link Target#USER_PERMISSIONS} 使用
     */
    public PermissionCacheResetCommand(Target target, Long userId) {
        this.target = target;
        this.userId = userId;
    }

    /**
     * 创建目标权限缓存重置命令。
     */
    public static PermissionCacheResetCommand targetPermissions() {
        return new PermissionCacheResetCommand(Target.TARGET_PERMISSIONS, null);
    }

    /**
     * 创建公开访问权限缓存重置命令。
     */
    public static PermissionCacheResetCommand publicAccessPermissions() {
        return new PermissionCacheResetCommand(Target.PUBLIC_ACCESS_PERMISSIONS, null);
    }

    /**
     * 创建指定用户权限缓存重置命令。
     *
     * @param userId 用户 ID
     */
    public static PermissionCacheResetCommand userPermissions(Long userId) {
        return new PermissionCacheResetCommand(Target.USER_PERMISSIONS, userId);
    }

    /**
     * 创建全部权限缓存重置命令。
     */
    public static PermissionCacheResetCommand all() {
        return new PermissionCacheResetCommand(Target.ALL, null);
    }

    /**
     * 权限缓存重置目标。
     */
    public enum Target {
        /** 目标权限缓存。 */
        TARGET_PERMISSIONS,
        /** 公开访问权限缓存。 */
        PUBLIC_ACCESS_PERMISSIONS,
        /** 指定用户权限缓存。 */
        USER_PERMISSIONS,
        /** 全部权限缓存。 */
        ALL
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionCacheResetCommand that = (PermissionCacheResetCommand) o;
        return target == that.target && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, userId);
    }
}
