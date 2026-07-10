package com.gnilc.auth.authn.servlet.config;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthenticationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletAuthenticationAutoConfiguration.class));

    // 没有认证处理器时，不注册认证过滤器，保持认证能力可选。
    // TestCaseId: CORE-AUTHN-008
    @Test
    void doesNotRegisterServletAuthenticationFilterWithoutServletAuthenticationHandler() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServletAuthenticationFilter.class);
            assertThat(authenticationFilterRegistrations(context.getBeansOfType(FilterRegistrationBean.class))).isEmpty();
        });
    }

    // 存在认证处理器时，自动注册认证过滤器并放在授权过滤器之前。
    // TestCaseId: CORE-AUTHN-009
    @Test
    void registerServletAuthenticationFilterWhenServletAuthenticationHandlerExists() {
        contextRunner.withUserConfiguration(ServletAuthenticationHandlerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAuthenticationFilter.class);
                    Map<String, FilterRegistrationBean> registrations = authenticationFilterRegistrations(context.getBeansOfType(FilterRegistrationBean.class));
                    assertThat(registrations).hasSize(1);
                    FilterRegistrationBean registration = registrations.values().iterator().next();
                    assertThat(registration.getFilter()).isInstanceOf(ServletAuthenticationFilter.class);
                    assertThat(registration.getUrlPatterns()).containsExactly("/*");
                    assertThat(registration.getOrder()).isEqualTo(ServletAuthenticationAutoConfiguration.AUTHENTICATION_FILTER_ORDER);
                });
    }

    // 应用提供认证失败处理器时，默认处理器让位。
    // TestCaseId: CORE-AUTHN-010
    @Test
    void customServletAuthenticationFailureHandlerOverridesDefaultHandler() {
        contextRunner.withUserConfiguration(ServletAuthenticationHandlerConfiguration.class, CustomFailureHandlerConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(CustomServletAuthenticationFailureHandler.class));
    }

    private Map<String, FilterRegistrationBean> authenticationFilterRegistrations(Map<String, FilterRegistrationBean> registrations) {
        return registrations.entrySet().stream()
                .filter(entry -> entry.getValue().getFilter() instanceof ServletAuthenticationFilter)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Configuration(proxyBeanMethods = false)
    static class ServletAuthenticationHandlerConfiguration {
        @Bean
        ServletAuthenticationHandler authenticationHandler() {
            return new ServletAuthenticationHandler() {
                @Override
                public boolean supports(ServletAuthenticationContext context) {
                    return true;
                }

                @Override
                public AuthenticationResult authenticate(ServletAuthenticationContext context) {
                    return AuthenticationResult.authenticated(DefaultAccessPrincipal.of("1001"));
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFailureHandlerConfiguration {
        @Bean
        CustomServletAuthenticationFailureHandler authenticationFailureHandler() {
            return new CustomServletAuthenticationFailureHandler();
        }
    }

    static class CustomServletAuthenticationFailureHandler implements ServletAuthenticationFailureHandler {
        @Override
        public void handle(ServletAuthenticationContext context, AuthenticationResult result) {
        }
    }
}
