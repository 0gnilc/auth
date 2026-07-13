package com.gnilc.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlingConfigurationTest {

    @Test
    void commonCoreDoesNotRegisterExceptionHandlingByDefault() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.refresh();

            assertThat(context.getBeansOfType(RestExceptionControllerAdvice.class)).isEmpty();
        }
    }

    @Test
    void consumerCanExplicitlyImportExceptionHandling() {
        try (var context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class)) {
            assertThat(context.getBeansOfType(RestExceptionControllerAdvice.class)).hasSize(1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(RestExceptionHandlingConfiguration.class)
    static class ConsumerConfiguration {
    }
}
