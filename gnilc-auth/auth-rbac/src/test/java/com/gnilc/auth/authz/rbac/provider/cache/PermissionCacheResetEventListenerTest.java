package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.provider.cache.redis.PermissionCacheRedisResetTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionCacheResetEventListenerTest {
    private PermissionCacheResetPolicy policy;
    private PermissionCacheResetExecutor executor;
    private PermissionCacheRedisResetTransport redisTransport;

    /**
     * Sets up fresh reset-policy collaborators before each test.
     */
    @BeforeEach
    void setUp() {
        policy = mock(PermissionCacheResetPolicy.class);
        executor = mock(PermissionCacheResetExecutor.class);
        redisTransport = mock(PermissionCacheRedisResetTransport.class);
    }

    /**
     * 本地节点收到 RBAC 授权事件后，应先执行本地缓存重置，再发布同一条命令到 Redis。
     * 这样本节点不依赖 Redis 回环消息完成 reset，同时其他节点仍能通过 Redis 收到标准化命令。
     */
    // TestCaseId: RBAC-CACHE-015
    @Test
    void executeLocallyBeforePublishingRemoteCommand() {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.targetPermissions();
        RbacAuthzEvent<Void> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR);
        when(policy.commandsForAll(event)).thenReturn(List.of(command));
        PermissionCacheResetEventListener listener = new PermissionCacheResetEventListener(policy, executor, Optional.of(redisTransport));

        listener.handleAll(event);

        inOrder(executor, redisTransport).verify(executor).execute(command);
        inOrder(executor, redisTransport).verify(redisTransport).publish(command);
    }

    /**
     * Redis transport 是可选能力：单机部署或未配置 Redis 时不应影响本地缓存重置。
     * 该用例验证 Optional.empty 场景下 listener 仍然执行本地 reset，且不会尝试发布远程消息。
     */
    // TestCaseId: RBAC-CACHE-016
    @Test
    void executeLocallyWhenRedisTransportIsMissing() {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.all();
        RbacAuthzEvent<Void> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR);
        when(policy.commandsForAll(event)).thenReturn(List.of(command));
        PermissionCacheResetEventListener listener = new PermissionCacheResetEventListener(policy, executor, Optional.empty());

        listener.handleAll(event);

        verify(executor).execute(command);
        verifyNoInteractions(redisTransport);
    }
}
