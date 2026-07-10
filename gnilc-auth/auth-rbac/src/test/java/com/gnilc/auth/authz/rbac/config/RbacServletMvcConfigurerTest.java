package com.gnilc.auth.authz.rbac.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class RbacServletMvcConfigurerTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RbacServletMvcConfigurer.class));

    // RBAC 默认 CORS Filter registration 应支持应用同名覆盖。
    // TestCaseId: RBAC-CONFIG-003
    @Test
    void keepsApplicationProvidedCorsFilterRegistration() {
        contextRunner.withUserConfiguration(CustomCorsFilterRegistrationConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("rbacCorsFilterRegistration");
                    assertThat(context.getBean("rbacCorsFilterRegistration", FilterRegistrationBean.class).getOrder())
                            .isEqualTo(123);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCorsFilterRegistrationConfiguration {
        @Bean("rbacCorsFilterRegistration")
        FilterRegistrationBean<CorsFilter> rbacCorsFilterRegistration() {
            FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
            registration.setOrder(123);
            return registration;
        }
    }
}
