package com.gnilc.auth.authz.rbac.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RbacServletMvcConfigurerTest {
    @Test
    void corsFilterAllowsCredentialedPreflightRequestsForEveryRoute() throws Exception {
        FilterRegistrationBean<CorsFilter> registration =
                new RbacServletMvcConfigurer().corsFilterRegistration();
        MockMvc mockMvc = standaloneSetup(new CorsProbeController())
                .addFilters(registration.getFilter())
                .build();

        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");

        mockMvc.perform(options("/test/cors")
                        .header(HttpHeaders.ORIGIN, "https://client.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Trace-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://client.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "PATCH"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "X-Trace-Id"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void keepsAnApplicationProvidedCorsRegistrationWithTheDocumentedBeanName() {
        FilterRegistrationBean<CorsFilter> customRegistration = new FilterRegistrationBean<>();

        new ApplicationContextRunner()
                .withUserConfiguration(RbacServletMvcConfigurer.class)
                .withBean("rbacCorsFilterRegistration", FilterRegistrationBean.class,
                        () -> customRegistration)
                .run(context -> assertThat(context.getBean("rbacCorsFilterRegistration"))
                        .isSameAs(customRegistration));
    }

    @RestController
    static class CorsProbeController {
        @GetMapping("/test/cors")
        void probe() {
        }
    }
}
