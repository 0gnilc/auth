package com.gnilc.auth.authz.config;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.decision.AffirmativeAccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.provider.DelegatingGrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.DelegatingRequiredPermissionsProvider;
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
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class));

    @Test
    void alwaysProvidesAccessDeniedButWaitsForBothProviderTypesBeforeDecision() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccessDenied.class);
            assertThat(context).doesNotHaveBean(AccessDecision.class);
            assertThat(context).doesNotHaveBean(DelegatingGrantedPermissionsProvider.BEAN_NAME);
            assertThat(context).doesNotHaveBean(DelegatingRequiredPermissionsProvider.BEAN_NAME);
        });
    }

    @Test
    void composesProvidersAndCreatesAffirmativeDecision() {
        contextRunner
                .withUserConfiguration(ProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DelegatingGrantedPermissionsProvider.class);
                    assertThat(context).hasSingleBean(DelegatingRequiredPermissionsProvider.class);
                    assertThat(context).getBean(AccessDecision.class)
                            .isInstanceOf(AffirmativeAccessDecision.class);
                    assertThat(context.getBean(AccessDecision.class).decide(new AccessContext(null, null)))
                            .isTrue();
                });
    }

    @Test
    void oneSidedProvidersDoNotCreateDecision() {
        contextRunner
                .withUserConfiguration(GrantedProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean(DelegatingGrantedPermissionsProvider.BEAN_NAME);
                    assertThat(context).doesNotHaveBean(DelegatingRequiredPermissionsProvider.BEAN_NAME);
                    assertThat(context).doesNotHaveBean(AccessDecision.class);
                });

        contextRunner
                .withUserConfiguration(RequiredProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DelegatingGrantedPermissionsProvider.BEAN_NAME);
                    assertThat(context).hasBean(DelegatingRequiredPermissionsProvider.BEAN_NAME);
                    assertThat(context).doesNotHaveBean(AccessDecision.class);
                });
    }

    @Test
    void backsOffApplicationProvidedDecision() {
        contextRunner
                .withUserConfiguration(ProviderConfiguration.class, CustomDecisionConfiguration.class)
                .run(context -> assertThat(context).getBean(AccessDecision.class)
                        .isSameAs(context.getBean("customDecision")));
    }

    @Configuration(proxyBeanMethods = false)
    static class GrantedProviderConfiguration {
        @Bean
        GrantedPermissionsProvider grantedPermissionsProvider() {
            return context -> List.of(new Permission("read"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RequiredProviderConfiguration {
        @Bean
        RequiredPermissionsProvider requiredPermissionsProvider() {
            return context -> List.of(new Permission("read"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDecisionConfiguration {
        @Bean
        AccessDecision customDecision() {
            return context -> false;
        }
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
