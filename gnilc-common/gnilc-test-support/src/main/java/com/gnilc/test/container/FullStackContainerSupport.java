package com.gnilc.test.container;

/**
 * 汇总 MySQL 与 Redis 测试容器连接属性的无状态入口。
 */
public final class FullStackContainerSupport {
    private FullStackContainerSupport() {
    }

    /**
     * 启动所需容器并依次输出 MySQL、Redis 及清理安全标记属性。
     *
     * @param properties 属性键值接收器
     */
    public static void applyProperties(java.util.function.BiConsumer<String, Object> properties) {
        MySqlContainerSupport.applyProperties(properties);
        RedisContainerSupport.applyProperties(properties);
    }
}
