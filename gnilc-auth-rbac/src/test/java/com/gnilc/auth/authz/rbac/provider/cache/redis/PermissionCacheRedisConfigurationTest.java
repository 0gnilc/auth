package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionCacheRedisConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PermissionCacheRedisConfiguration.class))
            .withPropertyValues("spring.main.lazy-initialization=true");

    // Redis reset 配置应仅在 RedisTemplate 和 RedisConnectionFactory 都存在时注册。
    // TestCaseId: RBAC-CACHE-029
    @Test
    void registersRedisResetBeansWhenRedisInfrastructureExists() {
        contextRunner.withUserConfiguration(NoopLifecycleProcessorConfiguration.class,
                        RedisInfrastructureConfiguration.class,
                        PermissionCacheResetExecutorConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PermissionCacheRedisResetTransport.class);
                    assertThat(context).hasBean("permissionCacheResetListenerContainer");
                    assertThat(context.getBean("permissionCacheResetListenerContainer", RedisMessageListenerContainer.class))
                            .isNotNull();
                });
    }

    // 应用显式提供 Redis reset transport 时，默认 transport 应让位。
    // TestCaseId: RBAC-CACHE-030
    @Test
    void keepsApplicationProvidedRedisResetTransport() {
        contextRunner.withUserConfiguration(NoopLifecycleProcessorConfiguration.class,
                        RedisInfrastructureConfiguration.class,
                        PermissionCacheResetExecutorConfiguration.class,
                        CustomRedisResetTransportConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PermissionCacheRedisResetTransport.class);
                    assertThat(context.getBean(PermissionCacheRedisResetTransport.class))
                            .isSameAs(context.getBean("customPermissionCacheRedisResetTransport"));
                });
    }

    // 应用显式提供 Redis listener container 时，默认 listener container 应让位。
    // TestCaseId: RBAC-CACHE-031
    @Test
    void keepsApplicationProvidedRedisListenerContainer() {
        contextRunner.withUserConfiguration(NoopLifecycleProcessorConfiguration.class,
                        RedisInfrastructureConfiguration.class,
                        PermissionCacheResetExecutorConfiguration.class,
                        CustomRedisListenerContainerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("permissionCacheResetListenerContainer");
                    assertThat(context.getBean("permissionCacheResetListenerContainer", RedisMessageListenerContainer.class))
                            .isNotNull();
                });
    }

    // 缺少 Redis infrastructure 时不注册 Redis reset beans。
    // TestCaseId: RBAC-CACHE-032
    @Test
    void doesNotRegisterRedisResetBeansWithoutRedisInfrastructure() {
        contextRunner.withUserConfiguration(PermissionCacheResetExecutorConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PermissionCacheRedisResetTransport.class);
                    assertThat(context).doesNotHaveBean("permissionCacheResetListenerContainer");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class NoopLifecycleProcessorConfiguration {
        @Bean("lifecycleProcessor")
        LifecycleProcessor lifecycleProcessor() {
            return mock(LifecycleProcessor.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RedisInfrastructureConfiguration {
        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
            RedisConnection connection = mock(RedisConnection.class);
            when(connectionFactory.getConnection()).thenReturn(connection);
            when(connection.isSubscribed()).thenReturn(false);
            return connectionFactory;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class PermissionCacheResetExecutorConfiguration {
        @Bean
        PermissionCacheResetExecutor permissionCacheResetExecutor() {
            return new PermissionCacheResetExecutor(new NoopPermissionCache());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRedisResetTransportConfiguration {
        @Bean
        PermissionCacheRedisResetTransport customPermissionCacheRedisResetTransport(StringRedisTemplate redisTemplate,
                                                                                    PermissionCacheResetExecutor resetExecutor) {
            return new PermissionCacheRedisResetTransport(redisTemplate, resetExecutor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRedisListenerContainerConfiguration {
        @Bean("permissionCacheResetListenerContainer")
        RedisMessageListenerContainer customPermissionCacheResetListenerContainer() {
            RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
            return container;
        }
    }

    private static class NoopPermissionCache implements PermissionCache {
        @Override
        public List<com.gnilc.auth.authz.rbac.provider.TargetPermission> loadTargetPermissions() {
            return List.of();
        }

        @Override
        public void resetTargetPermissions() {
        }

        @Override
        public List<com.gnilc.auth.authz.provider.Permission> loadUserPermissions(Long userId) {
            return List.of();
        }

        @Override
        public void resetUserPermissions(Long userId) {
        }

        @Override
        public List<com.gnilc.auth.authz.provider.Permission> loadPublicAccessPermissions() {
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
