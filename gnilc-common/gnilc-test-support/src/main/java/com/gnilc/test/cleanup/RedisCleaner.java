package com.gnilc.test.cleanup;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 清空测试 Redis 当前选定数据库中的全部键。
 */
public final class RedisCleaner {
    private final RedisConnectionFactory connectionFactory;

    /**
     * @param connectionFactory 连接测试 Redis 的连接工厂
     */
    public RedisCleaner(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /** 对当前 Redis 数据库执行 {@code FLUSHDB}。 */
    public void flushDatabase() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }
}
