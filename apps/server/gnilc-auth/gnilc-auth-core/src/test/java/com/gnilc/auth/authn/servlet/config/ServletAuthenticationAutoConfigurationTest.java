package com.gnilc.auth.authn.servlet.config;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthenticationAutoConfigurationTest {
    @Test
    void createsFilterOnlyWhenApplicationProvidesAHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(ServletAuthenticationFilter.class));

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class))
                .withUserConfiguration(AuthenticationHandlerConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(ServletAuthenticationFilter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class AuthenticationHandlerConfiguration {
        @Bean
        ServletAuthenticationHandler authenticationHandler() {
            return new ServletAuthenticationHandler() {
                @Override
                public boolean supports(ServletAuthenticationContext context) {
                    return false;
                }

                @Override
                public AuthenticationResult authenticate(ServletAuthenticationContext context) {
                    return AuthenticationResult.failed("not used");
                }
            };
        }
    }
}
