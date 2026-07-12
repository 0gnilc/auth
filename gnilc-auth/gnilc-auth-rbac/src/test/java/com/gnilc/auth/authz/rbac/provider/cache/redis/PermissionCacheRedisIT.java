package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import com.gnilc.test.cleanup.RedisCleaner;
import com.gnilc.test.container.RedisContainerContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = PermissionCacheRedisIT.RedisTestConfiguration.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RedisContainerContextInitializer.class)
class PermissionCacheRedisIT {
    @Autowired private StringRedisTemplate redis;
    @Autowired private RedisConnectionFactory connectionFactory;

    @AfterEach
    void cleanRedis() {
        new RedisCleaner(connectionFactory).flushDatabase();
    }

    @Test
    void commandPublishedByOneNodeIsExecutedByAnotherNode() throws Exception {
        PermissionCacheResetCommand command = PermissionCacheResetCommand.userPermissions(77L);
        PermissionCacheResetExecutor receiverExecutor = mock(PermissionCacheResetExecutor.class);
        PermissionCacheRedisResetTransport receiver =
                new PermissionCacheRedisResetTransport(redis, receiverExecutor);
        PermissionCacheRedisResetTransport publisher =
                new PermissionCacheRedisResetTransport(redis, mock(PermissionCacheResetExecutor.class));
        RedisMessageListenerContainer listener = new RedisMessageListenerContainer();
        listener.setConnectionFactory(connectionFactory);
        listener.addMessageListener(receiver,
                new ChannelTopic(PermissionCacheRedisResetTransport.RESET_CHANNEL));
        listener.afterPropertiesSet();
        listener.start();
        try {
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                publisher.publish(command);
                verify(receiverExecutor, atLeastOnce()).execute(command);
            });
        } finally {
            listener.stop();
            listener.destroy();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(RedisAutoConfiguration.class)
    static class RedisTestConfiguration {
    }
}
