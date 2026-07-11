package com.gnilc.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 将测试容器产生的动态连接属性写入 Spring 应用上下文。
 */
public final class ContainerContextProperties {
    private ContainerContextProperties() {
    }

    /**
     * 收集属性贡献器输出的键值对，并在上下文刷新前加入测试属性源。
     *
     * @param applicationContext 待初始化的 Spring 应用上下文
     * @param propertyContributor 接收属性写入器并贡献容器连接属性的函数
     */
    public static void applyTo(ConfigurableApplicationContext applicationContext,
                               Consumer<BiConsumer<String, Object>> propertyContributor) {
        List<String> values = new ArrayList<>();
        propertyContributor.accept((key, value) -> values.add(key + "=" + value));
        TestPropertyValues.of(values).applyTo(applicationContext);
    }
}
