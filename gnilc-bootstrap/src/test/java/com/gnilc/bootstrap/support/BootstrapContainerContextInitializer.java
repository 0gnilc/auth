package com.gnilc.bootstrap.support;

import com.gnilc.test.container.ContainerContextProperties;
import com.gnilc.test.container.FullStackContainerSupport;
import com.gnilc.test.container.MySqlContainerSupport;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class BootstrapContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContainerContextProperties.applyTo(applicationContext, FullStackContainerSupport::applyProperties);
        MySqlContainerSupport.initializeSchema(
                "sql/schema/01-rbac.sql",
                "sql/schema/02-admin.sql");
    }
}
