package com.gnilc.authz.rbac.provider.cache.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PermissionCacheRedisResetTransportTest {
    private StringRedisTemplate redisTemplate;
    private PermissionCacheResetExecutor executor;
    private PermissionCacheRedisResetTransport transport;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        executor = mock(PermissionCacheResetExecutor.class);
        transport = new PermissionCacheRedisResetTransport(redisTemplate, executor);
    }

    /**
     * 发布 reset command 时，transport 应把命令包装成 Redis message 并发送到统一 reset channel。
     * 该用例重点验证三件事：
     * <ul>
     *     <li>channel 使用 transport 暴露的协议常量，避免测试和实现各自硬编码不同通道；</li>
     *     <li>messageId 存在，便于未来通过日志追踪单条 Redis reset 消息；</li>
     *     <li>nodeId 带有 reset node 命名空间，用于识别消息发布节点。</li>
     * </ul>
     */
    @Test
    void publishResetCommandToResetChannel() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.userPermissions(1001L);

        transport.publish(command);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(PermissionCacheRedisResetTransport.RESET_CHANNEL), payloadCaptor.capture());
        PermissionCacheResetRedisMessage message = new ObjectMapper().readValue(payloadCaptor.getValue(), PermissionCacheResetRedisMessage.class);
        assertThat(message.getNodeId()).startsWith("access:permission:cache-reset:node:");
        assertThat(message.getMessageId()).isNotBlank();
        assertThat(message.getCommand()).isEqualTo(command);
    }

    /**
     * 收到远端节点发布的 Redis reset message 时，transport 应解码并执行其中的 command。
     * 这里不再重新推导业务事件影响范围，远端节点只执行已经标准化好的 reset command。
     */
    @Test
    void executeRemoteResetCommand() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.userPermissions(1001L);
        PermissionCacheResetRedisMessage message = new PermissionCacheResetRedisMessage("message-1", "node-remote", command);

        transport.onMessage(message(new ObjectMapper().writeValueAsString(message)), null);

        verify(executor).execute(command);
    }

    /**
     * Redis Pub/Sub 可能让发布节点收到自己刚发布的消息。
     * 本节点已经在 Spring 事件链路中执行过本地 reset，因此同节点消息应被忽略，避免重复执行和重复安排二次 reset。
     */
    @Test
    void ignoreMessagesFromSameNode() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.all();
        transport.publish(command);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(PermissionCacheRedisResetTransport.RESET_CHANNEL), payloadCaptor.capture());
        PermissionCacheResetRedisMessage localMessage = new ObjectMapper().readValue(payloadCaptor.getValue(), PermissionCacheResetRedisMessage.class);

        transport.onMessage(message(new ObjectMapper().writeValueAsString(localMessage)), null);

        verify(executor, never()).execute(command);
    }

    /**
     * Redis 消息可能因为配置错误、版本不一致或外部干扰而无法反序列化。
     * transport 对非法消息应安全忽略，不应把异常抛出到 Redis listener 容器，也不应触发任何 cache reset。
     */
    @Test
    void ignoreMalformedMessages() {
        transport.onMessage(message("not-json"), null);

        verify(executor, never()).execute(null);
    }

    /**
     * 构造 Spring Redis Message。
     * DefaultMessage 的第一个参数是 channel，第二个参数才是 body。
     */
    private Message message(String body) {
        return new DefaultMessage(new byte[0], body.getBytes(StandardCharsets.UTF_8));
    }
}
