package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PermissionCacheRedisResetTransportTest {
    @Test
    void publishedMessageContainsCommandAndSelfDeliveryIsIgnored() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        PermissionCacheResetExecutor executor = mock(PermissionCacheResetExecutor.class);
        PermissionCacheRedisResetTransport transport =
                new PermissionCacheRedisResetTransport(redis, executor);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);

        transport.publish(PermissionCacheResetCommand.userPermissions(9L));

        verify(redis).convertAndSend(
                org.mockito.ArgumentMatchers.eq(PermissionCacheRedisResetTransport.RESET_CHANNEL),
                payload.capture());
        PermissionCacheResetRedisMessage decoded =
                new ObjectMapper().readValue(payload.getValue(), PermissionCacheResetRedisMessage.class);
        assertThat(decoded.getCommand()).isEqualTo(PermissionCacheResetCommand.userPermissions(9L));

        Message sameNode = mock(Message.class);
        org.mockito.Mockito.when(sameNode.getBody())
                .thenReturn(payload.getValue().getBytes(StandardCharsets.UTF_8));
        transport.onMessage(sameNode, null);
        verify(executor, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void remoteMessagesExecuteAndMalformedMessagesAreIgnored() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        PermissionCacheResetExecutor executor = mock(PermissionCacheResetExecutor.class);
        PermissionCacheRedisResetTransport transport =
                new PermissionCacheRedisResetTransport(redis, executor);
        ObjectMapper mapper = new ObjectMapper();
        PermissionCacheResetRedisMessage remote = new PermissionCacheResetRedisMessage(
                "m1", "other-node", PermissionCacheResetCommand.all());
        Message message = mock(Message.class);
        org.mockito.Mockito.when(message.getBody()).thenReturn(mapper.writeValueAsBytes(remote));

        transport.onMessage(message, null);

        verify(executor).execute(PermissionCacheResetCommand.all());

        Message malformed = mock(Message.class);
        org.mockito.Mockito.when(malformed.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));
        transport.onMessage(malformed, null);
    }
}
