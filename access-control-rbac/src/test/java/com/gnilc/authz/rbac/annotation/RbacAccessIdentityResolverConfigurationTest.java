package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import com.gnilc.authz.rbac.context.AccessIdentityResolverDelegate;
import com.gnilc.authz.rbac.context.CompositeAccessIdentityResolver;
import com.gnilc.authz.web.context.ServletAccessIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RbacAccessIdentityResolverConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RbacAccessIdentityResolverConfiguration.class);

    /**
     * RBAC 身份解析配置应提供组合解析器作为默认 Resolver。
     */
    @Test
    void registerCompositeResolverByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccessIdentityResolver.class);
            assertThat(context.getBean(AccessIdentityResolver.class)).isInstanceOf(CompositeAccessIdentityResolver.class);
        });
    }

    /**
     * core 匿名 Resolver 不应阻止 RBAC 组合 Resolver 注册。
     */
    @Test
    void registerCompositeResolverWhenOnlyCoreDefaultResolverExists() {
        contextRunner.withUserConfiguration(CoreDefaultResolverConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("servletAccessIdentityResolver");
                    assertThat(context).hasBean("rbacAccessIdentityResolver");
                    assertThat(context.getBean(AccessIdentityResolver.class)).isInstanceOf(CompositeAccessIdentityResolver.class);
                });
    }

    /**
     * 应用显式提供完整 Resolver 时，RBAC 组合 Resolver 不应覆盖它。
     */
    @Test
    void keepApplicationProvidedResolver() {
        contextRunner.withUserConfiguration(CustomResolverConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AccessIdentityResolver.class);
                    assertThat(context.getBean(AccessIdentityResolver.class)).isSameAs(context.getBean("customAccessIdentityResolver"));
                });
    }

    /**
     * 应用提供 Delegate 时，应被组合 Resolver 纳入身份解析链路。
     */
    @Test
    @SuppressWarnings("unchecked")
    void useCustomDelegateInCompositeResolver() {
        contextRunner.withUserConfiguration(CustomDelegateConfiguration.class)
                .run(context -> {
                    AccessIdentityResolver<HttpServletRequest> resolver = context.getBean(AccessIdentityResolver.class);
                    AccessIdentity identity = resolver.resolve(mock(HttpServletRequest.class));

                    assertThat(identity.getIdentifier()).isEqualTo("1001");
                    assertThat(identity.getAttributes()).containsEntry("source", "custom");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreDefaultResolverConfiguration {
        @Bean
        AccessIdentityResolver<HttpServletRequest> servletAccessIdentityResolver() {
            return new ServletAccessIdentityResolver();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomResolverConfiguration {
        @Bean
        AccessIdentityResolver<HttpServletRequest> customAccessIdentityResolver() {
            return request -> new AccessIdentity("2001", Map.of("source", "customResolver"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDelegateConfiguration {
        @Bean
        AccessIdentityResolverDelegate<HttpServletRequest> customAccessIdentityResolverDelegate() {
            return new AccessIdentityResolverDelegate<>() {
                @Override
                public boolean supports(HttpServletRequest source) {
                    return true;
                }

                @Override
                public AccessIdentity resolve(HttpServletRequest source) {
                    return new AccessIdentity("1001", Map.of("source", "custom"));
                }
            };
        }
    }
}
