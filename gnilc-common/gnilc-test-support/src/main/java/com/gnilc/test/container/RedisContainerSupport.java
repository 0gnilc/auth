package com.gnilc.test.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.BiConsumer;

/**
 * 进程级 Redis Testcontainers 生命周期与动态属性支持。
 * <p>
 * 默认启动 {@code redis:8-alpine}，使用数据库 0 和不可复用容器。
 * 可通过系统属性 {@code app.test.redis.image} 覆盖镜像。
 */
public final class RedisContainerSupport {
    /** Redis 容器内部服务端口。 */
    public static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> CONTAINER = new GenericContainer<>(redisImage())
            .withExposedPorts(REDIS_PORT)
            .withReuse(false);

    private RedisContainerSupport() {
    }

    /**
     * 获取当前进程共享的 Redis 容器，并在首次访问时启动。
     *
     * @return 已启动的 Redis 测试容器
     */
    public static synchronized GenericContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    /**
     * 启动 Redis 容器并输出 Spring Redis、清理开关和容器归属属性。
     *
     * @param properties 属性键值接收器
     */
    public static void applyProperties(BiConsumer<String, Object> properties) {
        GenericContainer<?> redis = container();
        applyProperties(properties, redis.getHost(), redis.getMappedPort(REDIS_PORT));
    }

    /**
     * 根据给定主机和端口组装属性；该重载供无容器单元测试验证属性契约。
     */
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
