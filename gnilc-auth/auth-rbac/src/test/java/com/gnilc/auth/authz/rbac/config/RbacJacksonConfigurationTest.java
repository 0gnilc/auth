package com.gnilc.auth.authz.rbac.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RbacJacksonConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RbacJacksonConfiguration.class));

    // RBAC Jackson customizer 不应因应用存在其他 Jackson customizer 而丢失。
    // TestCaseId: RBAC-CONFIG-001
    @Test
    void registersRbacJacksonCustomizerWhenOtherCustomizerExists() {
        contextRunner.withUserConfiguration(OtherJacksonCustomizerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("otherJackson2ObjectMapperBuilderCustomizer");
                    assertThat(context).hasBean("rbacJackson2ObjectMapperBuilderCustomizer");
                    assertThat(context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class)).hasSize(2);
                });
    }

    // 应用显式提供同名 RBAC Jackson customizer 时，默认 customizer 应让位。
    // TestCaseId: RBAC-CONFIG-002
    @Test
    void keepsApplicationProvidedRbacJacksonCustomizerByBeanName() {
        contextRunner.withUserConfiguration(CustomRbacJacksonCustomizerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Jackson2ObjectMapperBuilderCustomizer.class);
                    assertThat(context).hasBean("rbacJackson2ObjectMapperBuilderCustomizer");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class OtherJacksonCustomizerConfiguration {
        @Bean
        Jackson2ObjectMapperBuilderCustomizer otherJackson2ObjectMapperBuilderCustomizer() {
            return builder -> {
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRbacJacksonCustomizerConfiguration {
        @Bean("rbacJackson2ObjectMapperBuilderCustomizer")
        Jackson2ObjectMapperBuilderCustomizer customRbacCustomizer() {
            return builder -> {
            };
        }
    }
}
