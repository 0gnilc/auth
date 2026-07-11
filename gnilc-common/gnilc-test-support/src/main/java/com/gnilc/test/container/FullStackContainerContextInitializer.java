package com.gnilc.test.container;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 在 Spring 测试上下文刷新前启动 MySQL 与 Redis 容器并注册动态连接属性。
 */
public class FullStackContainerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContainerContextProperties.applyTo(applicationContext, FullStackContainerSupport::applyProperties);
    }
}
