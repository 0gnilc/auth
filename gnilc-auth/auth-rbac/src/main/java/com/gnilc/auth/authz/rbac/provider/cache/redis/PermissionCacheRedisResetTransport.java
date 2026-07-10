package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis 权限缓存重置传输。
 * <p>
 * 统一处理重置命令的发布、订阅、编解码和同节点消息过滤。
 */
@Slf4j
public class PermissionCacheRedisResetTransport implements MessageListener {
    /**
     * 权限缓存重置消息通道。
     */
    public static final String RESET_CHANNEL = "access:permission:cache-reset:channel:v1";

    /**
     * Redis 字符串模板，用于发布重置消息。
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * 本地缓存重置执行器，用于执行远端节点发布的重置命令。
     */
    private final PermissionCacheResetExecutor resetExecutor;
    /**
     * Redis 消息编解码器，作为传输模块内部实现细节。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 当前节点标识，用于忽略本节点发布后又订阅到的消息。
     */
    private final String nodeId = "access:permission:cache-reset:node:" + UUID.randomUUID();

    /**
     * 创建 Redis 权限缓存重置传输。
     *
     * @param redisTemplate Redis 字符串模板
     * @param resetExecutor 权限缓存重置执行器
     */
    public PermissionCacheRedisResetTransport(StringRedisTemplate redisTemplate,
                                              PermissionCacheResetExecutor resetExecutor) {
        this.redisTemplate = redisTemplate;
        this.resetExecutor = resetExecutor;
    }

    /**
     * 发布缓存重置命令到 Redis reset 通道。
     *
     * @param command 缓存重置命令
     */
    public void publish(PermissionCacheResetCommand command) {
        if (command == null) {
            return;
        }
        PermissionCacheResetRedisMessage message = new PermissionCacheResetRedisMessage(
                UUID.randomUUID().toString(),
                nodeId,
                command
        );
        redisTemplate.convertAndSend(RESET_CHANNEL, encode(message));
    }

    /**
     * 处理 Redis 订阅消息。
     *
     * @param message Redis 消息
     * @param pattern 匹配模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        decode(message.getBody()).ifPresentOrElse(this::executeRemoteMessage,
                () -> log.warn("Ignore malformed permission cache reset message"));
    }

    private void executeRemoteMessage(PermissionCacheResetRedisMessage message) {
        if (nodeId.equals(message.getNodeId())) {
            return;
        }
        resetExecutor.execute(message.getCommand());
    }

    private String encode(PermissionCacheResetRedisMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to encode permission cache reset message", e);
        }
    }

    private Optional<PermissionCacheResetRedisMessage> decode(byte[] body) {
        try {
            PermissionCacheResetRedisMessage message = objectMapper.readValue(
                    new String(body, StandardCharsets.UTF_8),
                    PermissionCacheResetRedisMessage.class
            );
            return Optional.of(message);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
