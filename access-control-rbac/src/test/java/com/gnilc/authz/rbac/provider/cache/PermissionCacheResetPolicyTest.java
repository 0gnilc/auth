package com.gnilc.authz.rbac.provider.cache;

import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.gnilc.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionCacheResetPolicyTest {
    private RolePermissionService rolePermissionService;
    private UserRoleService userRoleService;
    private PermissionCacheResetPolicy policy;

    /**
     * Sets up a fresh reset policy before each test.
     */
    @BeforeEach
    void setUp() {
        rolePermissionService = mock(RolePermissionService.class);
        userRoleService = mock(UserRoleService.class);
        policy = new PermissionCacheResetPolicy(rolePermissionService, userRoleService);
    }

    /**
     * 权限数据变更会同时影响三类缓存：
     * <ul>
     *     <li>目标权限缓存：权限的 targetIdentifier 或 code 变化会影响 required permissions；</li>
     *     <li>公开访问权限缓存：权限的 publicAccess 状态变化会影响 anonymous/granted permissions；</li>
     *     <li>用户权限缓存：拥有该权限的用户 granted permissions 需要重置。</li>
     * </ul>
     * 该用例还验证同一用户通过多个角色关联到同一权限时，只生成一次用户权限重置命令。
     */
    @Test
    void createTargetPublicAccessAndUserCommandsForPermissionEvent() {
        when(rolePermissionService.getRoleIds(10L)).thenReturn(List.of(1L, 2L));
        when(userRoleService.getUserIds(List.of(1L, 2L))).thenReturn(List.of(100L, 100L, 200L));

        List<PermissionCacheResetCommand> commands = policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.UPDATE,
                10L));

        assertThat(commands).containsExactly(
                PermissionCacheResetCommand.targetPermissions(),
                PermissionCacheResetCommand.publicAccessPermissions(),
                PermissionCacheResetCommand.userPermissions(100L),
                PermissionCacheResetCommand.userPermissions(200L)
        );
    }

    /**
     * 角色本身变化会影响拥有该角色的用户权限。
     * 策略不直接重置角色缓存，而是把 role data 转换为受影响 userIds，生成用户权限缓存重置命令。
     */
    @Test
    void createUserCommandsForRoleEvent() {
        when(userRoleService.getUserIds(1L)).thenReturn(List.of(100L, 200L));

        List<PermissionCacheResetCommand> commands = policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.UPDATE,
                1L));

        assertThat(commands).containsExactly(
                PermissionCacheResetCommand.userPermissions(100L),
                PermissionCacheResetCommand.userPermissions(200L)
        );
    }

    /**
     * 角色与权限的绑定关系变化，会改变该角色下所有用户的 granted permissions。
     * 该用例验证 ROLE_PERMISSION 事件通过 role data 定位用户，而不是在 Redis 远端再次推导关系。
     */
    @Test
    void createUserCommandsForRolePermissionEvent() {
        when(userRoleService.getUserIds(1L)).thenReturn(List.of(100L));

        RbacAuthzEvent<Long> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE_PERMISSION,
                RbacAuthzEvent.Action.REPLACE,
                1L);

        List<PermissionCacheResetCommand> commands = policy.commandsFor(event);

        assertThat(commands).containsExactly(PermissionCacheResetCommand.userPermissions(100L));
    }

    /**
     * 用户角色绑定变化和用户自身变化都只影响单个用户的权限缓存。
     * 这两个事件不需要额外查询角色或权限关系，直接使用事件 data 生成用户权限重置命令。
     */
    @Test
    void createSingleUserCommandForUserRoleAndUserEvents() {
        RbacAuthzEvent<Long> userRoleEvent = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                100L);

        assertThat(policy.commandsFor(userRoleEvent))
                .containsExactly(PermissionCacheResetCommand.userPermissions(100L));
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER,
                RbacAuthzEvent.Action.DELETE,
                100L)))
                .containsExactly(PermissionCacheResetCommand.userPermissions(100L));
    }

    /**
     * ALL 事件表示外部明确要求清理 provider 权限缓存。
     * 策略应直接生成 all command，让 executor 统一执行全量重置。
     */
    @Test
    void createAllCommandForClearEvent() {
        assertThat(policy.commandsForAll(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR)))
                .containsExactly(PermissionCacheResetCommand.all());
    }

    /**
     * 业务事件可能携带空 data，例如调用方未传入或上游数据异常。
     * 策略层应安全忽略无法定位影响范围的用户/角色关系，但 PERMISSION 仍需要重置全局的目标权限和公开访问权限缓存。
     */
    @Test
    void ignoreNullDataWithoutThrowing() {
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.UPDATE,
                (Long) null)))
                .containsExactly(
                        PermissionCacheResetCommand.targetPermissions(),
                        PermissionCacheResetCommand.publicAccessPermissions()
                );
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.UPDATE,
                (Long) null))).isEmpty();
        assertThat(policy.commandsFor(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                (Long) null))).isEmpty();
    }
}
