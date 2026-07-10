package com.gnilc.test.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.BiConsumer;

public final class RedisContainerSupport {
    public static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> CONTAINER = new GenericContainer<>(redisImage())
            .withExposedPorts(REDIS_PORT)
            .withReuse(false);

    private RedisContainerSupport() {
    }

    public static synchronized GenericContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    public static void applyProperties(BiConsumer<String, Object> properties) {
        GenericContainer<?> redis = container();
        applyProperties(properties, redis.getHost(), redis.getMappedPort(REDIS_PORT));
    }

    static void applyProperties(BiConsumer<String, Object> properties, String host, int port) {
        properties.accept("spring.data.redis.host", host);
        properties.accept("spring.data.redis.port", port);
        properties.accept("spring.data.redis.database", 0);
        properties.accept("app.test.cleanup.enabled", true);
        properties.accept("app.test.container.owned", true);
        properties.accept("app.test.container.redis.host", host);
        properties.accept("app.test.container.redis.port", port);
    }

    private static DockerImageName redisImage() {
        return DockerImageName.parse(System.getProperty("app.test.redis.image", "redis:8-alpine"));
    }
}
