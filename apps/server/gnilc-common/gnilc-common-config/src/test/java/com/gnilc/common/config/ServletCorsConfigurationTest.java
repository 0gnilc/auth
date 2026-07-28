package com.gnilc.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class ServletCorsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletCorsConfiguration.class));

    @Test
    void registersTheExistingGlobalCorsPolicy() throws Exception {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<CorsFilter> registration =
                    (FilterRegistrationBean<CorsFilter>) context.getBean(FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
            assertThat(registration.getUrlPatterns()).containsExactly("/*");

            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/test");
            request.addHeader(HttpHeaders.ORIGIN, "https://client.example");
            request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
            MockHttpServletResponse response = new MockHttpServletResponse();

            registration.getFilter().doFilter(request, response, new MockFilterChain());

            assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .isEqualTo("https://client.example");
            assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                    .isEqualTo("true");
            assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
                    .isEqualTo("3600");
        });
    }

    @Test
    void backsOffWhenACorsFilterAlreadyExists() {
        CorsFilter customFilter = new CorsFilter(new UrlBasedCorsConfigurationSource());

        contextRunner
                .withBean("customCorsFilter", CorsFilter.class, () -> customFilter)
                .run(context -> {
                    assertThat(context).hasSingleBean(CorsFilter.class);
                    assertThat(context).doesNotHaveBean("servletCorsFilterRegistration");
                });
    }

    @Test
    void backsOffWhenACorsFilterRegistrationAlreadyExists() {
        contextRunner
                .withUserConfiguration(CustomCorsRegistrationConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("servletCorsFilterRegistration");
                    assertThat(context.getBeansOfType(FilterRegistrationBean.class))
                            .containsOnlyKeys("customCorsFilterRegistration");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCorsRegistrationConfiguration {
        @Bean
        FilterRegistrationBean<CorsFilter> customCorsFilterRegistration() {
            CorsFilter filter = new CorsFilter(new UrlBasedCorsConfigurationSource());
            return new FilterRegistrationBean<>(filter);
        }
    }
}
