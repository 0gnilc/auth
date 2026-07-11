package com.gnilc.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;

public final class MySqlContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        MySQLContainer<?> mysql = SharedTestContainers.mysql();
        TestPropertyValues.of(
                "spring.datasource.url=" + mysql.getJdbcUrl(),
                "spring.datasource.username=" + mysql.getUsername(),
                "spring.datasource.password=" + mysql.getPassword(),
                "spring.datasource.driver-class-name=" + mysql.getDriverClassName(),
                "spring.sql.init.mode=never",
                "app.test.cleanup.enabled=true"
        ).applyTo(context);
    }
}
