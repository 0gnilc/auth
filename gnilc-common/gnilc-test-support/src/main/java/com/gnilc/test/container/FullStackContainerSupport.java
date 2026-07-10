package com.gnilc.test.container;

public final class FullStackContainerSupport {
    private FullStackContainerSupport() {
    }

    public static void applyProperties(java.util.function.BiConsumer<String, Object> properties) {
        MySqlContainerSupport.applyProperties(properties);
        RedisContainerSupport.applyProperties(properties);
    }
}
