package com.gnilc.auth.config;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.config.ServletAuthenticationAutoConfiguration;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import com.gnilc.auth.authz.servlet.config.ServletAuthorizationAutoConfiguration;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAutoConfigurationTest {
    @Test
    void authorizationCoreBuildsDecisionOnlyWhenBothProviderTypesExist() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class))
                .withUserConfiguration(ProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AccessDecision.class);
                    assertThat(context).hasSingleBean(AccessDenied.class);
                });

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AccessDecision.class);
                    assertThat(context).hasSingleBean(AccessDenied.class);
                });
    }

    @Test
    void authenticationFilterExistsOnlyWhenApplicationProvidesAHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(ServletAuthenticationFilter.class));

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class))
                .withUserConfiguration(AuthenticationHandlerConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(ServletAuthenticationFilter.class));
    }

    @Test
    void authorizationServletBeansAreConditionalOnDecisionAndDeniedEntryPoint() {
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
    static class ProviderConfiguration {
        @Bean
        GrantedPermissionsProvider grantedPermissionsProvider() {
            return context -> List.of(new Permission("read"));
        }

        @Bean
        RequiredPermissionsProvider requiredPermissionsProvider() {
            return context -> List.of(new Permission("read"));
        }
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

    @Configuration(proxyBeanMethods = false)
    static class DecisionConfiguration {
        @Bean
        AccessDecision accessDecision() {
            return context -> true;
        }

        @Bean
        AccessDenied accessDenied() {
            return (accessContext, deniedContext) -> { };
        }
    }
}
