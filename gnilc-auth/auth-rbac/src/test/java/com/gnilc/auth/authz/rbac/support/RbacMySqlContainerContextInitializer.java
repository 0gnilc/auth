package com.gnilc.auth.authz.rbac.support;

import com.gnilc.test.container.ContainerContextProperties;
import com.gnilc.test.container.MySqlContainerSupport;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class RbacMySqlContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContainerContextProperties.applyTo(applicationContext, MySqlContainerSupport::applyProperties);
        MySqlContainerSupport.initializeSchema("sql/schema/01-rbac.sql");
    }
}
