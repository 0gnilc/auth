package com.gnilc.auth.authz.servlet.config;

import com.gnilc.auth.FilterRegistrationOrder;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessContextAdapter;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessIdentityResolver;
import com.gnilc.auth.authz.context.AccessIdentityResolverHandler;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.context.AccessTargetResolver;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.ServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.context.ServletRequestContext;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAuthorizationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class, ServletAuthorizationAutoConfiguration.class))
            .withUserConfiguration(CoreDependencyConfiguration.class);

    // 默认 Servlet 授权自动配置注册 Servlet 命名的 adapter/helper/filter。
    // TestCaseId: CORE-SERVLET-014
    @Test
    void registerDefaultServletAuthorizationBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("defaultServletAccessIdentityResolverHandler");
            assertThat(context.getBean(DefaultServletAccessIdentityResolverHandler.class))
                    .isInstanceOf(DefaultServletAccessIdentityResolverHandler.class);
            assertThat(context).hasBean("servletAccessIdentityResolver");
            assertThat(context.getBean("servletAccessIdentityResolver")).isInstanceOf(DefaultServletAccessIdentityResolver.class);
            assertThat(context).hasBean("servletAccessTargetResolver");
            assertThat(context.getBean("servletAccessTargetResolver")).isInstanceOf(DefaultServletAccessTargetResolver.class);
            assertThat(context).hasSingleBean(ServletAccessContextAdapter.class);
            assertThat(context.getBean(ServletAccessContextAdapter.class)).isInstanceOf(DefaultServletAccessContextAdapter.class);
            assertThat(context).hasSingleBean(ServletAuthorizationFilter.class);

            Map<String, FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class);
            assertThat(registrations).containsKey("servletAuthorizationFilterRegistration");
            FilterRegistrationBean<Filter> registration = registrations.get("servletAuthorizationFilterRegistration");
            assertThat(registration.getFilter()).isSameAs(context.getBean(ServletAuthorizationFilter.class));
            assertThat(registration.getOrder()).isEqualTo(FilterRegistrationOrder.SERVLET_AUTHORIZATION_FILTER_ORDER);
            assertThat(registration.getUrlPatterns()).containsExactly("/*");
        });
    }

    // 非 Servlet 的 core adapter/resolver/handler 不应阻止 Servlet 默认 bean 注册，也不应被 Servlet filter 注入。
    // TestCaseId: CORE-SERVLET-015
    @Test
    void keepServletAuthorizationBeansWhenNonServletCoreBeansExist() {
        contextRunner.withUserConfiguration(NonServletAccessComponentsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("messageAccessContextAdapter");
                    assertThat(context).hasBean("messageAccessIdentityResolver");
                    assertThat(context).hasBean("messageAccessIdentityResolverHandler");
                    assertThat(context).hasBean("messageAccessTargetResolver");
                    assertThat(context).hasSingleBean(ServletAccessIdentityResolver.class);
                    assertThat(context).hasSingleBean(ServletAccessTargetResolver.class);
                    assertThat(context).hasSingleBean(ServletAccessContextAdapter.class);
                    assertThat(context.getBean(ServletAccessIdentityResolver.class)).isInstanceOf(DefaultServletAccessIdentityResolver.class);
                    assertThat(context.getBean(ServletAccessContextAdapter.class)).isInstanceOf(DefaultServletAccessContextAdapter.class);

                    Map<String, FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class);
                    assertThat(registrations.get("servletAuthorizationFilterRegistration").getFilter())
                            .isInstanceOf(ServletAuthorizationFilter.class);
                });
    }

    // Servlet handler 应纳入默认 Servlet 身份解析器，并优先于默认兜底 handler。
    // TestCaseId: CORE-SERVLET-016
    @Test
    void useServletAccessIdentityResolverHandlerInDefaultResolver() {
        contextRunner.withUserConfiguration(ServletAccessIdentityResolverHandlerConfiguration.class)
                .run(context -> {
                    ServletAccessIdentityResolver resolver = context.getBean(ServletAccessIdentityResolver.class);

                    AccessIdentity identity = resolver.resolve(servletRequestContext(DefaultAccessPrincipal.of("default-user")));

                    assertThat(resolver).isInstanceOf(DefaultServletAccessIdentityResolver.class);
                    assertThat(identity.getIdentifier()).isEqualTo("1001");
                    assertThat(identity.getAttributes()).containsEntry("source", "handler");
                });
    }

    // 默认 Servlet 身份解析处理器应解析认证 Principal。
    // TestCaseId: CORE-SERVLET-017
    @Test
    void defaultServletAccessIdentityResolverHandlerResolvesPrincipal() {
        contextRunner.run(context -> {
            ServletAccessIdentityResolver resolver = context.getBean(ServletAccessIdentityResolver.class);

            AccessIdentity identity = resolver.resolve(servletRequestContext(DefaultAccessPrincipal.of(1001L, Map.of("source", "authn"))));

            assertThat(identity.getIdentifier()).isEqualTo("1001");
            assertThat(identity.getAttributes()).containsEntry("source", "authn");
            assertThat(identity.getAttributes()).containsEntry("principal", true);
        });
    }

    // 默认 Servlet 身份解析处理器应排在不支持当前请求的自定义 handler 之后。
    // TestCaseId: CORE-SERVLET-018
    @Test
    void defaultServletAccessIdentityResolverHandlerRunsAfterUnsupportedCustomHandlers() {
        contextRunner.withUserConfiguration(UnsupportedServletAccessIdentityResolverHandlerConfiguration.class)
                .run(context -> {
                    ServletAccessIdentityResolver resolver = context.getBean(ServletAccessIdentityResolver.class);

                    AccessIdentity identity = resolver.resolve(servletRequestContext(DefaultAccessPrincipal.of("1001")));

                    assertThat(identity.getIdentifier()).isEqualTo("1001");
                    assertThat(identity.getAttributes()).containsEntry("principal", true);
                });
    }

    // 应用显式提供 Servlet adapter 时，默认 Servlet adapter 不应覆盖它。
    // TestCaseId: CORE-SERVLET-019
    @Test
    void keepApplicationProvidedServletAccessContextAdapter() {
        contextRunner.withUserConfiguration(CustomServletAccessContextAdapterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAccessContextAdapter.class);
                    assertThat(context.getBean(ServletAccessContextAdapter.class))
                            .isSameAs(context.getBean("customServletAccessContextAdapter"));
                });
    }

    // 应用显式提供 ServletAuthorizationFilter 时，默认 Filter 不应覆盖它。
    // TestCaseId: CORE-SERVLET-020
    @Test
    void keepApplicationProvidedServletAuthorizationFilter() {
        contextRunner.withUserConfiguration(CustomServletAuthorizationFilterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServletAuthorizationFilter.class);
                    assertThat(context.getBean(ServletAuthorizationFilter.class))
                            .isSameAs(context.getBean("customServletAuthorizationFilter"));
                    assertThat(context.getBean("servletAuthorizationFilterRegistration", FilterRegistrationBean.class).getFilter())
                            .isSameAs(context.getBean("customServletAuthorizationFilter"));
                });
    }

    // 应用显式提供 ServletAuthorizationFilter registration 时，默认 registration 不应覆盖它。
    // TestCaseId: CORE-SERVLET-021
    @Test
    void keepApplicationProvidedServletAuthorizationFilterRegistration() {
        contextRunner.withUserConfiguration(CustomServletAuthorizationFilterRegistrationConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("servletAuthorizationFilterRegistration");
                    assertThat(context.getBean("servletAuthorizationFilterRegistration", FilterRegistrationBean.class).getOrder())
                            .isEqualTo(123);
                });
    }

    // 没有 AccessDecision 时不注册 Servlet 授权 Filter，避免 classpath SPI 场景下误装配授权链。
    // TestCaseId: CORE-SERVLET-022
    @Test
    void doesNotRegisterServletAuthorizationFilterWithoutAccessDecision() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuthorizationAutoConfiguration.class, ServletAuthorizationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AccessDecision.class);
                    assertThat(context).doesNotHaveBean(ServletAuthorizationFilter.class);
                    assertThat(context).doesNotHaveBean("servletAuthorizationFilterRegistration");
                });
    }

    private ServletRequestContext servletRequestContext(java.security.Principal principal) {
        FilterChain chain = (request, response) -> {
        };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(principal);
        return new ServletRequestContext(request, new MockHttpServletResponse(), chain);
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreDependencyConfiguration {
        @Bean
        AccessDecision accessDecision() {
            return context -> true;
        }

        @Bean
        AccessDenied accessDenied() {
            return (accessContext, deniedContext) -> {
            };
        }

        @Bean
        GrantedPermissionsProvider grantedPermissionsProvider() {
            return context -> List.of();
        }

        @Bean
        RequiredPermissionsProvider requiredPermissionsProvider() {
            return context -> List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NonServletAccessComponentsConfiguration {
        @Bean
        AccessContextAdapter<String> messageAccessContextAdapter() {
            return message -> new AccessContext(
                    AccessEnvironment.of("message"),
                    new AccessIdentity("message-user", Map.of()),
                    new AccessTarget("topic.auth", null, Map.of())
            );
        }

        @Bean
        AccessIdentityResolver<String> messageAccessIdentityResolver() {
            return message -> new AccessIdentity("message-user", Map.of());
        }

        @Bean
        AccessIdentityResolverHandler<String> messageAccessIdentityResolverHandler() {
            return new AccessIdentityResolverHandler<>() {
                @Override
                public boolean supports(String source) {
                    return true;
                }

                @Override
                public AccessIdentity handle(String source) {
                    return new AccessIdentity("message-user", Map.of());
                }
            };
        }

        @Bean
        AccessTargetResolver<String> messageAccessTargetResolver() {
            return message -> new AccessTarget("topic.auth", null, Map.of());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ServletAccessIdentityResolverHandlerConfiguration {
        @Bean
        ServletAccessIdentityResolverHandler servletAccessIdentityResolverHandler() {
            return new ServletAccessIdentityResolverHandler() {
                @Override
                public boolean supports(ServletRequestContext source) {
                    return true;
                }

                @Override
                public AccessIdentity handle(ServletRequestContext source) {
                    return new AccessIdentity("1001", Map.of("source", "handler"));
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UnsupportedServletAccessIdentityResolverHandlerConfiguration {
        @Bean
        ServletAccessIdentityResolverHandler unsupportedServletAccessIdentityResolverHandler() {
            return new ServletAccessIdentityResolverHandler() {
                @Override
                public boolean supports(ServletRequestContext source) {
                    return false;
                }

                @Override
                public AccessIdentity handle(ServletRequestContext source) {
                    return new AccessIdentity("unsupported", Map.of("source", "unsupported"));
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomServletAccessContextAdapterConfiguration {
        @Bean
        ServletAccessContextAdapter customServletAccessContextAdapter() {
            return request -> new AccessContext(
                    AccessEnvironment.SERVLET,
                    new AccessIdentity("custom-user", Map.of()),
                    new AccessTarget("/custom", null, Map.of())
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomServletAuthorizationFilterConfiguration {
        @Bean
        ServletAuthorizationFilter customServletAuthorizationFilter(AccessDecision accessDecision,
                                                                    ServletAccessContextAdapter accessContextAdapter,
                                                                    AccessDenied accessDenied) {
            return new ServletAuthorizationFilter(accessDecision, accessContextAdapter, accessDenied);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomServletAuthorizationFilterRegistrationConfiguration {
        @Bean("servletAuthorizationFilterRegistration")
        FilterRegistrationBean<Filter> customRegistration() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setFilter((request, response, chain) -> chain.doFilter(request, response));
            registration.setOrder(123);
            return registration;
        }
    }
}
