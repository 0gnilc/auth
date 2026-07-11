package com.gnilc.test.container;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 在 Spring 测试上下文刷新前启动 Redis 容器并注册动态连接属性。
 */
public class RedisContainerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContainerContextProperties.applyTo(applicationContext, RedisContainerSupport::applyProperties);
    }
}
