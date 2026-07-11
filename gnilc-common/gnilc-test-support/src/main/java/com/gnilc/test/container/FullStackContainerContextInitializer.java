package com.gnilc.test.container;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class FullStackContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        new MySqlContainerContextInitializer().initialize(context);
        new RedisContainerContextInitializer().initialize(context);
    }
}
