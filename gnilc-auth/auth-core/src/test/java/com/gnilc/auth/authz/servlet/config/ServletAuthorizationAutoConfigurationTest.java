package com.gnilc.auth.authz.servlet.config;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthorizationAutoConfigurationTest {
    @Test
    void createsServletBeansOnlyWhenDecisionAndDeniedEntryPointExist() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthorizationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAccessContextAdapter.class);
                    assertThat(context).doesNotHaveBean(ServletAuthorizationFilter.class);
                });

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthorizationAutoConfiguration.class))
                .withUserConfiguration(DecisionConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(ServletAuthorizationFilter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class DecisionConfiguration {
        @Bean
        AccessDecision accessDecision() {
            return context -> true;
        }

        @Bean
        AccessDenied accessDenied() {
            return (context, deniedContext) -> { };
        }
    }
}
