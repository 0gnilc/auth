package com.gnilc.auth.authz.servlet.config;

import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.ServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.ServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration(after = {DispatcherServletAutoConfiguration.class, AuthorizationAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, Filter.class})
public class ServletAuthorizationAutoConfiguration {

    public static final int AUTHORIZATION_FILTER_ORDER = Integer.MAX_VALUE;

    /**
     * 创建默认 Servlet 访问身份解析处理器。
     * <p>
     * 该 handler 作为 Servlet handler 链最后兜底规则，将认证 Principal 转换为授权身份。
     */
    @Bean
    @ConditionalOnMissingBean(DefaultServletAccessIdentityResolverHandler.class)
    public DefaultServletAccessIdentityResolverHandler defaultServletAccessIdentityResolverHandler() {
        return new DefaultServletAccessIdentityResolverHandler();
    }

    /**
     * 创建默认 Servlet 身份解析辅助。
     * <p>
     * 该 bean 使用 Servlet 专用 seam 作为 Spring Bean 边界，并组合 Servlet handler，避免其他执行环境的
     * 身份解析器或 handler 影响 Servlet 授权配置。
     */
    @Bean
    @ConditionalOnMissingBean(ServletAccessIdentityResolver.class)
    public ServletAccessIdentityResolver servletAccessIdentityResolver(ObjectProvider<ServletAccessIdentityResolverHandler> handlers,
                                                                       DefaultServletAccessIdentityResolverHandler defaultHandler) {
        List<ServletAccessIdentityResolverHandler> orderedHandlers = new ArrayList<>(handlers.orderedStream()
                .filter(handler -> handler != defaultHandler)
                .toList());
        orderedHandlers.add(defaultHandler);
        return new DefaultServletAccessIdentityResolver(orderedHandlers);
    }

    /**
     * 创建默认 Servlet 访问目标解析辅助。
     */
    @Bean
    @ConditionalOnMissingBean(ServletAccessTargetResolver.class)
    public ServletAccessTargetResolver servletAccessTargetResolver() {
        return new DefaultServletAccessTargetResolver();
    }

    /**
     * 创建 Servlet 访问上下文 adapter。
     * <p>
     * ServletAuthorizationFilter 只依赖 Servlet 专用 adapter 进入授权核心；需要改变访问环境或上下文构造流程的应用
     * 应覆盖这个 bean，而不是提供其他执行环境的 AccessContextAdapter。
     */
    @Bean
    @ConditionalOnMissingBean(ServletAccessContextAdapter.class)
    public ServletAccessContextAdapter servletAccessContextAdapter(ServletAccessIdentityResolver accessIdentityResolver,
                                                                   ServletAccessTargetResolver accessTargetResolver) {
        return new DefaultServletAccessContextAdapter(accessIdentityResolver, accessTargetResolver);
    }

    @Bean
    @ConditionalOnBean({AccessDecision.class, AccessDenied.class})
    @ConditionalOnMissingBean(ServletAuthorizationFilter.class)
    public ServletAuthorizationFilter servletAuthorizationFilter(AccessDecision accessDecision,
                                                                 ServletAccessContextAdapter accessContextAdapter,
                                                                 AccessDenied accessDenied) {
        return new ServletAuthorizationFilter(accessDecision, accessContextAdapter, accessDenied);
    }

    @Bean
    @ConditionalOnBean(ServletAuthorizationFilter.class)
    @ConditionalOnMissingBean(name = "servletAuthorizationFilterRegistration")
    public FilterRegistrationBean<Filter> servletAuthorizationFilterRegistration(ServletAuthorizationFilter authorizationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authorizationFilter);
        registration.addUrlPatterns("/*");
        registration.setName(ServletAuthorizationFilter.class.getName());
        registration.setOrder(AUTHORIZATION_FILTER_ORDER);
        return registration;
    }
}
