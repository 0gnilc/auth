package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.provider.cache.redis.PermissionCacheRedisResetTransport;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

/**
 * RBAC 授权事件到权限缓存重置命令的入口。
 * <p>
 * 监听授权数据变化事件，在事务提交后重置本地缓存，并把同一命令发布到 Redis。
 */
@Component
public class PermissionCacheResetEventListener {
    private final PermissionCacheResetPolicy resetPolicy;
    private final PermissionCacheResetExecutor resetExecutor;
    private final Optional<PermissionCacheRedisResetTransport> redisResetTransport;

    /**
     * 创建权限缓存重置事件监听器。
     *
     * @param resetPolicy         缓存重置策略
     * @param resetExecutor       本地缓存重置执行器
     * @param redisResetTransport Redis 重置传输，可不存在
     */
    public PermissionCacheResetEventListener(PermissionCacheResetPolicy resetPolicy,
                                             PermissionCacheResetExecutor resetExecutor,
                                             Optional<PermissionCacheRedisResetTransport> redisResetTransport) {
        this.resetPolicy = resetPolicy;
        this.resetExecutor = resetExecutor;
        this.redisResetTransport = redisResetTransport;
    }

    /**
     * 处理携带授权数据 ID 的 RBAC 授权数据变化事件。
     *
     * @param event 携带授权数据 ID 的 RBAC 授权事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(RbacAuthzEvent<Long> event) {
        reset(resetPolicy.commandsFor(event));
    }

    /**
     * 处理无数据的 RBAC 授权全量清理事件。
     *
     * @param event RBAC 授权全量清理事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAll(RbacAuthzEvent<Void> event) {
        reset(resetPolicy.commandsForAll(event));
    }

    private void reset(List<PermissionCacheResetCommand> commands) {
        for (PermissionCacheResetCommand command : commands) {
            resetExecutor.execute(command);
            redisResetTransport.ifPresent(transport -> transport.publish(command));
        }
    }
}
