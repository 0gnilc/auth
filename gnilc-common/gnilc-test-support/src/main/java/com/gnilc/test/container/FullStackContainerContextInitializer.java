package com.gnilc.test.container;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class FullStackContainerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContainerContextProperties.applyTo(applicationContext, FullStackContainerSupport::applyProperties);
    }
}
