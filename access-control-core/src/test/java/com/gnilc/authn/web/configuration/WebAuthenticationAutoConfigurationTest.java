package com.gnilc.authn.web.configuration;

import com.gnilc.authn.web.context.ServletAuthenticationContext;
import com.gnilc.authn.web.filter.AuthenticationFilter;
import com.gnilc.authn.web.handler.AuthenticationFailureHandler;
import com.gnilc.authn.web.handler.AuthenticationHandler;
import com.gnilc.authn.web.handler.AuthenticationResult;
import com.gnilc.authz.web.filter.AuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebAuthenticationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebAuthenticationAutoConfiguration.class));

    // 没有认证处理器时，不注册认证过滤器，保持认证能力可选。
    @Test
    void doesNotRegisterAuthenticationFilterWithoutAuthenticationHandler() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AuthenticationFilter.class);
            assertThat(authenticationFilterRegistrations(context.getBeansOfType(FilterRegistrationBean.class))).isEmpty();
        });
    }

    // 存在认证处理器时，自动注册认证过滤器并放在授权过滤器之前。
    @Test
    void registerAuthenticationFilterWhenAuthenticationHandlerExists() {
        contextRunner.withUserConfiguration(AuthenticationHandlerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthenticationFilter.class);
                    Map<String, FilterRegistrationBean> registrations = authenticationFilterRegistrations(context.getBeansOfType(FilterRegistrationBean.class));
                    assertThat(registrations).hasSize(1);
                    FilterRegistrationBean registration = registrations.values().iterator().next();
                    assertThat(registration.getFilter()).isInstanceOf(AuthenticationFilter.class);
                    assertThat(registration.getUrlPatterns()).containsExactly("/*");
                    assertThat(registration.getOrder()).isEqualTo(AuthorizationFilter.REGISTRATION_ORDER_PREVIOUS);
                });
    }

    // 应用提供认证失败处理器时，默认处理器让位。
    @Test
    void customAuthenticationFailureHandlerOverridesDefaultHandler() {
        contextRunner.withUserConfiguration(AuthenticationHandlerConfiguration.class, CustomFailureHandlerConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(CustomAuthenticationFailureHandler.class));
    }

    private Map<String, FilterRegistrationBean> authenticationFilterRegistrations(Map<String, FilterRegistrationBean> registrations) {
        return registrations.entrySet().stream()
                .filter(entry -> entry.getValue().getFilter() instanceof AuthenticationFilter)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Configuration(proxyBeanMethods = false)
    static class AuthenticationHandlerConfiguration {
        @Bean
        AuthenticationHandler authenticationHandler() {
            return new AuthenticationHandler() {
                @Override
                public boolean supports(ServletAuthenticationContext context) {
                    return true;
                }

                @Override
                public AuthenticationResult authenticate(ServletAuthenticationContext context) {
                    return AuthenticationResult.authenticated(() -> "1001");
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFailureHandlerConfiguration {
        @Bean
        CustomAuthenticationFailureHandler authenticationFailureHandler() {
            return new CustomAuthenticationFailureHandler();
        }
    }

    static class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
        @Override
        public void handle(ServletAuthenticationContext context, AuthenticationResult result) {
        }
    }
}
