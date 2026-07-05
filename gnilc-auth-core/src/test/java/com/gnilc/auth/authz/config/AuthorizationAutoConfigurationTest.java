package com.gnilc.auth.authz.config;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.auth.authz.denied.DefaultAccessDenied;
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
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class));

    // TestCaseId: CORE-AUTHZ-045
    @Test
    void registerDefaultAccessDenied() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccessDenied.class);
            assertThat(context.getBean(AccessDenied.class)).isInstanceOf(DefaultAccessDenied.class);
        });
    }

    // TestCaseId: CORE-AUTHZ-046
    @Test
    void keepApplicationProvidedAccessDenied() {
        contextRunner.withUserConfiguration(CustomAccessDeniedConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AccessDenied.class);
                    assertThat(context.getBean(AccessDenied.class)).isSameAs(context.getBean("customAccessDenied"));
                });
    }

    // TestCaseId: CORE-AUTHZ-047
    @Test
    void collectOrderedAccessDeniedHandlersIntoDefaultAccessDenied() {
        contextRunner.withUserConfiguration(AccessDeniedHandlerConfiguration.class)
                .run(context -> {
                    AccessDenied accessDenied = context.getBean(AccessDenied.class);
                    List<String> called = context.getBean(AccessDeniedHandlerConfiguration.class).called;

                    accessDenied.denied(null, new TestAccessDeniedContext());

                    assertThat(called).containsExactly("earlier", "later");
                });
    }

    // TestCaseId: CORE-AUTHZ-048
    @Test
    void registerAccessDecisionWhenPermissionProvidersExist() {
        contextRunner.withUserConfiguration(ProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AccessDecision.class);
                    assertThat(context).hasBean(DelegatingGrantedPermissionsProvider.BEAN_NAME);
                    assertThat(context).hasBean(DelegatingRequiredPermissionsProvider.BEAN_NAME);
                    assertThat(context.getBean(DelegatingGrantedPermissionsProvider.BEAN_NAME))
                            .isInstanceOf(DelegatingGrantedPermissionsProvider.class);
                    assertThat(context.getBean(DelegatingRequiredPermissionsProvider.BEAN_NAME))
                            .isInstanceOf(DelegatingRequiredPermissionsProvider.class);
                });
    }

    // TestCaseId: CORE-AUTHZ-049
    @Test
    void skipAccessDecisionWhenPermissionProvidersAreMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AccessDecision.class);
            assertThat(context).doesNotHaveBean(DelegatingGrantedPermissionsProvider.class);
            assertThat(context).doesNotHaveBean(DelegatingRequiredPermissionsProvider.class);
        });
    }

    // TestCaseId: CORE-AUTHZ-050
    @Test
    void keepApplicationProvidedAccessDecision() {
        contextRunner.withUserConfiguration(CustomAccessDecisionConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AccessDecision.class);
                    assertThat(context.getBean(AccessDecision.class)).isSameAs(context.getBean("customAccessDecision"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderConfiguration {
        @Bean
        GrantedPermissionsProvider grantedPermissionsProvider() {
            return context -> List.of(new Permission("user:read"));
        }

        @Bean
        RequiredPermissionsProvider requiredPermissionsProvider() {
            return context -> List.of(new Permission("user:read"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAccessDeniedConfiguration {
        @Bean
        AccessDenied customAccessDenied() {
            return (context, deniedContext) -> {
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAccessDecisionConfiguration {
        @Bean
        AccessDecision customAccessDecision() {
            return context -> true;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AccessDeniedHandlerConfiguration {
        private final List<String> called = new ArrayList<>();

        @Bean
        AccessDeniedHandler laterAccessDeniedHandler() {
            return new OrderedHandler(20, "later", called);
        }

        @Bean
        AccessDeniedHandler earlierAccessDeniedHandler() {
            return new OrderedHandler(10, "earlier", called);
        }
    }

    private static class OrderedHandler implements AccessDeniedHandler, Ordered {
        private final int order;
        private final String name;
        private final List<String> called;

        private OrderedHandler(int order, String name, List<String> called) {
            this.order = order;
            this.name = name;
            this.called = called;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void handle(com.gnilc.auth.authz.context.AccessContext accessContext, AccessDeniedContext deniedContext) {
            called.add(name);
        }
    }

    private static class TestAccessDeniedContext implements AccessDeniedContext {
    }
}
