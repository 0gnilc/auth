package com.gnilc.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlingConfigurationIT {

    @Test
    void consumerCanExplicitlyImportExceptionHandling() {
        try (var context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class)) {
            assertThat(context.getBeansOfType(
                    RestExceptionHandlingConfiguration.RestExceptionControllerAdvice.class)).hasSize(1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(RestExceptionHandlingConfiguration.class)
    static class ConsumerConfiguration {
    }
}
