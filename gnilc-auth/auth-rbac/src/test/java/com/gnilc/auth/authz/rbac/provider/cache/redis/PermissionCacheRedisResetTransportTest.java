package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
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
import static org.mockito.Mockito.verifyNoInteractions;

class PermissionCacheRedisResetTransportTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringRedisTemplate redisTemplate;
    private PermissionCacheResetExecutor executor;
    private PermissionCacheRedisResetTransport transport;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        executor = mock(PermissionCacheResetExecutor.class);
        transport = new PermissionCacheRedisResetTransport(redisTemplate, executor);
    }

    @Test
    void publishesVersionedMessagesToTheResetChannel() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.userPermissions(42L);

        transport.publish(command);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(PermissionCacheRedisResetTransport.RESET_CHANNEL), payload.capture());
        PermissionCacheResetRedisMessage message = objectMapper.readValue(
                payload.getValue(), PermissionCacheResetRedisMessage.class);
        assertThat(message.getMessageId()).isNotBlank();
        assertThat(message.getNodeId()).startsWith("access:permission:cache-reset:node:");
        assertThat(message.getCommand()).isEqualTo(command);
    }

    @Test
    void executesCommandsPublishedByAnotherNode() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.all();
        PermissionCacheResetRedisMessage remoteMessage = new PermissionCacheResetRedisMessage(
                "message-1", "remote-node", command);

        transport.onMessage(message(objectMapper.writeValueAsString(remoteMessage)), null);

        verify(executor).execute(command);
    }

    @Test
    void ignoresMessagesPublishedByTheSameNode() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.targetPermissions();
        transport.publish(command);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(PermissionCacheRedisResetTransport.RESET_CHANNEL), payload.capture());

        transport.onMessage(message(payload.getValue()), null);

        verify(executor, never()).execute(command);
    }

    @Test
    void ignoresMalformedMessagesAndNullCommands() {
        transport.publish(null);
        transport.onMessage(message("not-json"), null);

        verifyNoInteractions(redisTemplate, executor);
    }

    private Message message(String body) {
        return new DefaultMessage(new byte[0], body.getBytes(StandardCharsets.UTF_8));
    }
}
