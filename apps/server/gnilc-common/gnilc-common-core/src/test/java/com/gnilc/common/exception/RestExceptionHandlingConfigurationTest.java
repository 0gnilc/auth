package com.gnilc.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlingConfigurationTest {

    @Test
    void broadComponentScanDoesNotDiscoverExceptionHandling() {
        var scanner = new ClassPathScanningCandidateComponentProvider(true);

        assertThat(scanner.findCandidateComponents("com.gnilc.common.exception"))
                .extracting(definition -> definition.getBeanClassName())
                .doesNotContain(
                        RestExceptionHandlingConfiguration.class.getName(),
                        RestExceptionHandlingConfiguration.RestExceptionControllerAdvice.class.getName());
    }
}
