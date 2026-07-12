package com.gnilc.auth.authn.servlet.config;

import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authn.servlet.handler.DefaultServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Servlet 认证自动配置。
 * <p>
 * 只有应用提供认证处理器时才注册认证过滤器。
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, Filter.class})
@ConditionalOnBean(ServletAuthenticationHandler.class)
@AutoConfigureAfter(DispatcherServletAutoConfiguration.class)
public class ServletAuthenticationAutoConfiguration {

    public static final int AUTHENTICATION_FILTER_ORDER = Integer.MAX_VALUE - 1;

    @Bean
    @ConditionalOnMissingBean
    public ServletAuthenticationFailureHandler authenticationFailureHandler() {
        return new DefaultServletAuthenticationFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServletAuthenticationFilter authenticationFilter(ObjectProvider<ServletAuthenticationHandler> handlers,
                                                     ServletAuthenticationFailureHandler failureHandler) {
        return new ServletAuthenticationFilter(handlers.orderedStream().toList(), failureHandler);
    }

    @Bean
    @ConditionalOnMissingBean(name = "authenticationFilterRegistration")
    public FilterRegistrationBean<Filter> authenticationFilterRegistration(ServletAuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setName(ServletAuthenticationFilter.class.getName());
        registration.setOrder(AUTHENTICATION_FILTER_ORDER);
        return registration;
    }
}
