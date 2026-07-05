package com.gnilc.auth.authz.rbac.provider.cache.redis;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheResetExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 权限缓存 Redis 重置传输配置。
 * <p>
 * 仅在 RedisTemplate 和 RedisConnectionFactory 同时存在时启用远程缓存重置能力。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({StringRedisTemplate.class, RedisConnectionFactory.class})
public class PermissionCacheRedisConfiguration {

    /**
     * 创建 Redis 重置传输模块。
     *
     * @param redisTemplate Redis 字符串模板
     * @param resetExecutor 权限缓存重置执行器
     * @return Redis 重置传输模块
     */
    @Bean
    @ConditionalOnMissingBean(PermissionCacheRedisResetTransport.class)
    public PermissionCacheRedisResetTransport permissionCacheRedisResetTransport(StringRedisTemplate redisTemplate,
                                                                                PermissionCacheResetExecutor resetExecutor) {
        return new PermissionCacheRedisResetTransport(redisTemplate, resetExecutor);
    }

    /**
     * 注册权限缓存重置消息监听容器。
     *
     * @param connectionFactory Redis 连接工厂
     * @param resetTransport    Redis 重置传输模块
     * @return Redis 消息监听容器
     */
    @Bean
    @ConditionalOnMissingBean(name = "permissionCacheResetListenerContainer")
    public RedisMessageListenerContainer permissionCacheResetListenerContainer(RedisConnectionFactory connectionFactory,
                                                                              PermissionCacheRedisResetTransport resetTransport) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(resetTransport, new ChannelTopic(PermissionCacheRedisResetTransport.RESET_CHANNEL));
        return container;
    }
}
