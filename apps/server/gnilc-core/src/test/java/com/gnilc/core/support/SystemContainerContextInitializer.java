package com.gnilc.core.support;

import com.gnilc.test.container.FullStackContainerContextInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class SystemContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // 类上的 @ContextConfiguration 会替换 @ApiTest 声明的初始化器，因此这里显式组合容器和业务 schema。
        new FullStackContainerContextInitializer().initialize(context);
        new SystemModuleContextInitializer().initialize(context);
    }
}
