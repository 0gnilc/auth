package com.gnilc.test.container;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 组合 MySQL 与 Redis 初始化器，供需要完整基础设施的集成测试使用。
 */
public final class FullStackContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * 按顺序向测试上下文注入 MySQL 和 Redis 连接信息。
     *
     * @param context 待初始化的 Spring 应用上下文
     */
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        new MySqlContainerContextInitializer().initialize(context);
        new RedisContainerContextInitializer().initialize(context);
    }
}
