package com.gnilc.auth.authz.rbac.support;

import com.gnilc.test.container.FullStackContainerContextInitializer;
import com.gnilc.test.container.SharedTestContainers;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class RbacContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        new FullStackContainerContextInitializer().initialize(context);
        SharedTestContainers.initializeMySqlSchema("sql/schema/01_rbac.sql");
        applyRbacProperties(context);
    }

    public static void applyRbacProperties(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        ).applyTo(context);
    }
}
