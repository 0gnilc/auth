package com.gnilc.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ContainerContextProperties {
    private ContainerContextProperties() {
    }

    public static void applyTo(ConfigurableApplicationContext applicationContext,
                               Consumer<BiConsumer<String, Object>> propertyContributor) {
        List<String> values = new ArrayList<>();
        propertyContributor.accept((key, value) -> values.add(key + "=" + value));
        TestPropertyValues.of(values).applyTo(applicationContext);
    }
}
