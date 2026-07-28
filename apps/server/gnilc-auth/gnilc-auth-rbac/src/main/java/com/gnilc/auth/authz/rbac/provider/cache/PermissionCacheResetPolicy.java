package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Stream;

/**
 * 权限缓存重置策略。
 * <p>
 * 负责把 RBAC 授权事件翻译成可直接执行和传输的标准化重置命令。
 */
@Component
public class PermissionCacheResetPolicy {
    /**
     * 角色权限服务，用于根据权限反查受影响角色。
     */
    private final RolePermissionService rolePermissionService;
    /**
     * 用户角色服务，用于根据角色反查受影响用户。
     */
    private final UserRoleService userRoleService;

    /**
     * 创建权限缓存重置策略。
     *
     * @param rolePermissionService 角色权限服务
     * @param userRoleService       用户角色服务
     */
    public PermissionCacheResetPolicy(RolePermissionService rolePermissionService,
                                      UserRoleService userRoleService) {
        this.rolePermissionService = rolePermissionService;
        this.userRoleService = userRoleService;
    }

    /**
     * 根据携带 Long 数据的 RBAC 授权事件生成缓存重置命令。
     *
     * @param event 携带授权数据 ID 的 RBAC 授权事件
     * @return 缓存重置命令列表
     */
    public List<PermissionCacheResetCommand> commandsFor(AuthorizationEvent<Long> event) {
        if (event == null || event.getType() == null) {
            return List.of();
        }
        return switch (event.getType()) {
            case PERMISSION -> commandsForPermission(event.getData());
            case ROLE, ROLE_PERMISSION -> userCommandsForRole(event.getData());
            case USER, USER_ROLE -> userCommand(event.getData());
            case ROLE_MENU, ALL -> List.of();
        };
    }

    /**
     * 根据无数据的全量清理事件生成缓存重置命令。
     *
     * @param event RBAC 授权全量清理事件
     * @return 缓存重置命令列表
     */
    public List<PermissionCacheResetCommand> commandsForAll(AuthorizationEvent<Void> event) {
        if (event == null || event.getType() != AuthorizationEvent.Type.ALL) {
            return List.of();
        }
        return List.of(PermissionCacheResetCommand.all());
    }

    /**
     * 权限变更会影响目标权限、公开访问权限，以及拥有该权限的用户权限。
     *
     * @param permissionId 权限 ID
     * @return 缓存重置命令列表
     */
    private List<PermissionCacheResetCommand> commandsForPermission(Long permissionId) {
        Stream<PermissionCacheResetCommand> globalCommands = Stream.of(
                PermissionCacheResetCommand.targetPermissions(),
                PermissionCacheResetCommand.publicAccessPermissions()
        );
        return Stream.concat(globalCommands, userCommandsForPermission(permissionId)
                .stream()).toList();
    }

    /**
     * 根据权限 ID 推导受影响用户的权限缓存重置命令。
     * <p>
     * 权限先影响角色，再通过角色影响用户权限缓存。
     *
     * @param permissionId 权限 ID
     * @return 用户权限缓存重置命令列表
     */
    private List<PermissionCacheResetCommand> userCommandsForPermission(Long permissionId) {
        if (permissionId == null) {
            return List.of();
        }
        List<Long> roleIds = rolePermissionService.getRoleIds(permissionId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return userCommands(userRoleService.getUserIds(roleIds));
    }

    /**
     * 根据角色 ID 推导受影响用户的权限缓存重置命令。
     *
     * @param roleId 角色 ID
     * @return 用户权限缓存重置命令列表
     */
    private List<PermissionCacheResetCommand> userCommandsForRole(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return userCommands(userRoleService.getUserIds(roleId));
    }

    /**
     * 创建单个用户权限缓存重置命令。
     *
     * @param userId 用户 ID
     * @return 用户权限缓存重置命令列表
     */
    private List<PermissionCacheResetCommand> userCommand(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return List.of(PermissionCacheResetCommand.userPermissions(userId));
    }

    /**
     * 创建多个用户权限缓存重置命令。
     * <p>
     * 对用户 ID 去重，避免重复重置同一用户缓存。
     *
     * @param userIds 用户 ID 列表
     * @return 用户权限缓存重置命令列表
     */
    private List<PermissionCacheResetCommand> userCommands(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }
        return userIds.stream()
                .distinct()
                .map(PermissionCacheResetCommand::userPermissions)
                .toList();
    }
}
