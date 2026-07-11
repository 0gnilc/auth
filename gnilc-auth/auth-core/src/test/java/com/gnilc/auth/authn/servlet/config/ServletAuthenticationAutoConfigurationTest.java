package com.gnilc.auth.authn.servlet.config;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authn.servlet.handler.DefaultServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthenticationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class));

    @Test
    void backsOffEntireAuthenticationFlowWithoutHandler() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServletAuthenticationFilter.class);
            assertThat(context).doesNotHaveBean(ServletAuthenticationFailureHandler.class);
            assertThat(context).doesNotHaveBean("authenticationFilterRegistration");
        });
    }

    @Test
    void registersDefaultFailureHandlerFilterAndOrderedRegistration() {
        contextRunner
                .withUserConfiguration(HandlerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAuthenticationFilter.class);
                    assertThat(context).getBean(ServletAuthenticationFailureHandler.class)
                            .isInstanceOf(DefaultServletAuthenticationFailureHandler.class);
                    FilterRegistrationBean<?> registration = context.getBean(
                            "authenticationFilterRegistration",
                            FilterRegistrationBean.class
                    );
                    assertThat(registration.getFilter()).isSameAs(context.getBean(ServletAuthenticationFilter.class));
                    assertThat(registration.getOrder())
                            .isEqualTo(ServletAuthenticationAutoConfiguration.AUTHENTICATION_FILTER_ORDER);
                    assertThat(registration.getUrlPatterns()).containsExactly("/*");
                    assertThat(registration.getFilterName()).isEqualTo(ServletAuthenticationFilter.class.getName());
                });
    }

    @Test
    void backsOffUserProvidedBeans() {
        contextRunner
                .withUserConfiguration(CustomAuthenticationConfiguration.class)
                .run(context -> {
                    assertThat(context).getBean(ServletAuthenticationFailureHandler.class)
                            .isSameAs(context.getBean("customFailureHandler"));
                    assertThat(context).getBean(ServletAuthenticationFilter.class)
                            .isSameAs(context.getBean("customAuthenticationFilter"));
                    assertThat(context).getBean(
                                    "authenticationFilterRegistration",
                                    FilterRegistrationBean.class
                            )
                            .extracting(FilterRegistrationBean::getOrder)
                            .isEqualTo(Integer.MAX_VALUE);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class HandlerConfiguration {
        @Bean
        ServletAuthenticationHandler authenticationHandler() {
            return new ServletAuthenticationHandler() {
                @Override
                public boolean supports(ServletAuthenticationContext context) {
                    return false;
                }

                @Override
                public AuthenticationResult authenticate(ServletAuthenticationContext context) {
                    throw new AssertionError("unsupported handler must not authenticate");
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAuthenticationConfiguration extends HandlerConfiguration {
        @Bean
        ServletAuthenticationFailureHandler customFailureHandler() {
            return (context, result) -> { };
        }

        @Bean
        ServletAuthenticationFilter customAuthenticationFilter(ServletAuthenticationHandler handler,
                                                                ServletAuthenticationFailureHandler failureHandler) {
            return new ServletAuthenticationFilter(java.util.List.of(handler), failureHandler);
        }

        @Bean(name = "authenticationFilterRegistration")
        FilterRegistrationBean<Filter> customAuthenticationFilterRegistration(
                ServletAuthenticationFilter filter
        ) {
            return new FilterRegistrationBean<>(filter);
        }
    }
}
