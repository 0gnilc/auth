package com.gnilc.test.cleanup;

import org.springframework.data.redis.connection.RedisConnectionFactory;

public class RedisCleaner {
    private final RedisConnectionFactory connectionFactory;

    public RedisCleaner(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void clean() {
        try (var connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }
}
