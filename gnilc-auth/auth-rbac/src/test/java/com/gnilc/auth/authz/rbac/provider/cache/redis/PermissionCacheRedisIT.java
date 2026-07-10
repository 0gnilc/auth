package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetCommand;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.test.container.RedisContainerContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ContextConfiguration(
        classes = PermissionCacheRedisIT.RedisTestConfiguration.class,
        initializers = RedisContainerContextInitializer.class
)
class PermissionCacheRedisIT {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisConnectionFactory connectionFactory;

    private RedisMessageListenerContainer listenerContainer;
    private RecordingPermissionCache receivingCache;

    @BeforeEach
    void setUp() {
        receivingCache = new RecordingPermissionCache();
        PermissionCacheRedisResetTransport receiver = new PermissionCacheRedisResetTransport(
                redisTemplate, new PermissionCacheResetExecutor(receivingCache));
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.addMessageListener(
                receiver,
                new ChannelTopic(PermissionCacheRedisResetTransport.RESET_CHANNEL)
        );
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        listenerContainer.stop();
        listenerContainer.destroy();
    }

    @Test
    void publishesAndReceivesResetCommandsThroughRealRedisPubSub() throws InterruptedException {
        PermissionCacheRedisResetTransport publisher = new PermissionCacheRedisResetTransport(
                redisTemplate, new PermissionCacheResetExecutor(new NoopPermissionCache()));

        publisher.publish(PermissionCacheResetCommand.userPermissions(42L));

        assertThat(receivingCache.awaitReset()).isTrue();
        assertThat(receivingCache.resetUserId()).isEqualTo(42L);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(RedisAutoConfiguration.class)
    static class RedisTestConfiguration {
    }

    private static final class RecordingPermissionCache extends NoopPermissionCache {
        private final CountDownLatch reset = new CountDownLatch(1);
        private final AtomicReference<Long> resetUserId = new AtomicReference<>();

        @Override
        public void resetUserPermissions(Long userId) {
            resetUserId.set(userId);
            reset.countDown();
        }

        boolean awaitReset() throws InterruptedException {
            return reset.await(10, TimeUnit.SECONDS);
        }

        Long resetUserId() {
            return resetUserId.get();
        }
    }

    private static class NoopPermissionCache implements PermissionCache {
        @Override
        public List<TargetPermission> loadTargetPermissions() {
            return List.of();
        }

        @Override
        public void resetTargetPermissions() {
        }

        @Override
        public List<Permission> loadUserPermissions(Long userId) {
            return List.of();
        }

        @Override
        public void resetUserPermissions(Long userId) {
        }

        @Override
        public List<Permission> loadPublicAccessPermissions() {
            return List.of();
        }

        @Override
        public void resetPublicAccessPermissions() {
        }

        @Override
        public void resetAll() {
        }
    }
}
