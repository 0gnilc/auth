package com.gnilc.auth.authz.config;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationAutoConfigurationTest {
    @Test
    void buildsDecisionOnlyWhenBothProviderTypesExist() {
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
}
