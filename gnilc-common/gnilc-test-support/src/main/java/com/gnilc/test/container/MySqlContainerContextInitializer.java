package com.gnilc.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class MySqlContainerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        List<String> values = new ArrayList<>();
        MySqlContainerSupport.applyProperties((key, value) -> values.add(key + "=" + value));
        TestPropertyValues.of(values).applyTo(applicationContext);
    }
}
