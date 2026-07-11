package com.gnilc.test.cleanup;

import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 清空当前 Redis 连接所指向数据库的测试数据。
 * <p>
 * 调用方必须先通过 {@link TestEnvironmentGuard} 验证 Redis 实例归测试容器所有。
 */
public class RedisCleaner {
    private final RedisConnectionFactory connectionFactory;

    /**
     * 创建 Redis 清理器。
     *
     * @param connectionFactory 指向测试容器 Redis 的连接工厂
     */
    public RedisCleaner(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * 对当前 Redis 数据库执行 {@code FLUSHDB}。
     */
    public void clean() {
        try (var connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }
}
