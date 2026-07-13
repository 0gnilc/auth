package com.gnilc.common.exception;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in configuration for the common REST exception handling policy.
 *
 * <p>Applications control activation by explicitly importing this configuration.</p>
 */
@Configuration(proxyBeanMethods = false)
public class RestExceptionHandlingConfiguration {

    @Bean
    RestExceptionControllerAdvice restExceptionControllerAdvice() {
        return new RestExceptionControllerAdvice();
    }
}
