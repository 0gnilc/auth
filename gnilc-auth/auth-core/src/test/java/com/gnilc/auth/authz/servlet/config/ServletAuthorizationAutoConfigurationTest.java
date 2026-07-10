package com.gnilc.auth.authz.servlet.config;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthorizationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletAuthorizationAutoConfiguration.class));

    @Test
    void providesServletContextInfrastructureWithoutDecision() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DefaultServletAccessIdentityResolverHandler.class);
            assertThat(context).getBean(ServletAccessIdentityResolver.class)
                    .isInstanceOf(DefaultServletAccessIdentityResolver.class);
            assertThat(context).getBean(ServletAccessTargetResolver.class)
                    .isInstanceOf(DefaultServletAccessTargetResolver.class);
            assertThat(context).getBean(ServletAccessContextAdapter.class)
                    .isInstanceOf(DefaultServletAccessContextAdapter.class);
            assertThat(context).doesNotHaveBean(ServletAuthorizationFilter.class);
            assertThat(context).doesNotHaveBean("servletAuthorizationFilterRegistration");
        });
    }

    @Test
    void registersFilterAfterDecisionAndDeniedEntryExist() {
        contextRunner
                .withUserConfiguration(AuthorizationDependencies.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAuthorizationFilter.class);
                    FilterRegistrationBean<?> registration = context.getBean(
                            "servletAuthorizationFilterRegistration",
                            FilterRegistrationBean.class
                    );
                    assertThat(registration.getFilter()).isSameAs(context.getBean(ServletAuthorizationFilter.class));
                    assertThat(registration.getOrder())
                            .isEqualTo(ServletAuthorizationAutoConfiguration.AUTHORIZATION_FILTER_ORDER);
                    assertThat(registration.getUrlPatterns()).containsExactly("/*");
                    assertThat(registration.getFilterName()).isEqualTo(ServletAuthorizationFilter.class.getName());
                });
    }

    @Test
    void backsOffCustomAdapterFilterAndRegistration() {
        contextRunner
                .withUserConfiguration(CustomServletAuthorizationConfiguration.class)
                .run(context -> {
                    assertThat(context).getBean(ServletAccessContextAdapter.class)
                            .isSameAs(context.getBean("customAdapter"));
                    assertThat(context).getBean(ServletAuthorizationFilter.class)
                            .isSameAs(context.getBean("customFilter"));
                    assertThat(context).getBean(
                                    "servletAuthorizationFilterRegistration",
                                    FilterRegistrationBean.class
                            )
                            .extracting(FilterRegistrationBean::getOrder)
                            .isEqualTo(Integer.MAX_VALUE);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class AuthorizationDependencies {
        @Bean
        AccessDecision accessDecision() {
            return context -> true;
        }

        @Bean
        AccessDenied accessDenied() {
            return (accessContext, deniedContext) -> { };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomServletAuthorizationConfiguration extends AuthorizationDependencies {
        @Bean
        ServletAccessContextAdapter customAdapter() {
            return source -> null;
        }

        @Bean
        ServletAuthorizationFilter customFilter(AccessDecision accessDecision,
                                                ServletAccessContextAdapter adapter,
                                                AccessDenied accessDenied) {
            return new ServletAuthorizationFilter(accessDecision, adapter, accessDenied);
        }

        @Bean(name = "servletAuthorizationFilterRegistration")
        FilterRegistrationBean<Filter> customRegistration(ServletAuthorizationFilter filter) {
            return new FilterRegistrationBean<>(filter);
        }
    }
}
