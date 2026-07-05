package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis 中传输的权限缓存重置消息。
 */
@Setter
@Getter
public class PermissionCacheResetRedisMessage {
    /**
     * 消息标识，用于日志追踪和排查 Redis 传输链路。
     */
    private String messageId;
    /**
     * 发布节点标识，用于忽略本节点发布后又订阅到的消息。
     */
    private String nodeId;
    /**
     * 标准化缓存重置命令，表示远端节点需要执行的重置动作。
     */
    private PermissionCacheResetCommand command;

    public PermissionCacheResetRedisMessage() {
    }

    /**
     * 创建 Redis 缓存重置消息。
     *
     * @param messageId 消息标识
     * @param nodeId    发布节点标识
     * @param command   缓存重置命令
     */
    public PermissionCacheResetRedisMessage(String messageId, String nodeId, PermissionCacheResetCommand command) {
        this.messageId = messageId;
        this.nodeId = nodeId;
        this.command = command;
    }

}
