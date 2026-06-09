package com.gnilc.authz.rbac.provider.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PermissionCacheResetExecutorTest {
    private PermissionCache permissionCache;
    private PermissionCacheResetExecutor executor;

    /**
     * Sets up a fresh reset executor before each test.
     */
    @BeforeEach
    void setUp() {
        permissionCache = mock(PermissionCache.class);
        executor = new PermissionCacheResetExecutor(permissionCache);
    }

    /**
     * TARGET_PERMISSIONS 命令表示目标权限数据发生变化。
     * executor 应把标准化命令转换为 PermissionCache.resetTargetPermissions 调用。
     */
    @Test
    void executeTargetPermissionResetCommand() {
        executor.execute(PermissionCacheResetCommand.targetPermissions());

        verify(permissionCache).resetTargetPermissions();
    }

    /**
     * PUBLIC_ACCESS_PERMISSIONS 命令表示公开访问权限数据发生变化。
     * executor 应只重置公开访问权限缓存，不需要影响用户权限或目标权限缓存。
     */
    @Test
    void executePublicAccessPermissionResetCommand() {
        executor.execute(PermissionCacheResetCommand.publicAccessPermissions());

        verify(permissionCache).resetPublicAccessPermissions();
    }

    /**
     * USER_PERMISSIONS 命令必须携带 userId。
     * executor 应把 userId 原样传递给 PermissionCache.resetUserPermissions。
     */
    @Test
    void executeUserPermissionResetCommand() {
        executor.execute(PermissionCacheResetCommand.userPermissions(100L));

        verify(permissionCache).resetUserPermissions(100L);
    }

    /**
     * 缺少 userId 的 USER_PERMISSIONS 命令无法定位具体用户缓存。
     * executor 应安全忽略，避免把 null 当成缓存 key 传给实现层。
     */
    @Test
    void ignoreUserPermissionResetWithoutUserId() {
        executor.execute(PermissionCacheResetCommand.userPermissions(null));

        verify(permissionCache, never()).resetUserPermissions(null);
    }

    /**
     * ALL 命令表示全量重置 provider 权限缓存。
     * executor 应委托 PermissionCache.resetAll，由缓存实现自行清理所有缓存分区。
     */
    @Test
    void executeAllCommand() {
        executor.execute(PermissionCacheResetCommand.all());

        verify(permissionCache).resetAll();
    }

    /**
     * Redis 或 Spring 事件链路中如果传入 null command，不应中断整个监听流程。
     * executor 对 null 命令保持幂等安全，直接忽略且不调用 PermissionCache。
     */
    @Test
    void ignoreNullCommand() {
        executor.execute(null);

        verifyNoInteractions(permissionCache);
    }
}
