package com.gnilc.authn.web.config;

import com.gnilc.authn.web.filter.AuthenticationFilter;
import com.gnilc.authn.web.handler.AuthenticationFailureHandler;
import com.gnilc.authn.web.handler.AuthenticationHandler;
import com.gnilc.authn.web.handler.DefaultAuthenticationFailureHandler;
import com.gnilc.authz.web.annotation.WebAuthorizationConfiguration;
import com.gnilc.authz.web.filter.AuthorizationFilter;
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
@ConditionalOnBean(AuthenticationHandler.class)
@AutoConfigureAfter({DispatcherServletAutoConfiguration.class, WebAuthorizationConfiguration.class})
public class WebAuthenticationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new DefaultAuthenticationFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationFilter authenticationFilter(ObjectProvider<AuthenticationHandler> handlers,
                                                     AuthenticationFailureHandler failureHandler) {
        return new AuthenticationFilter(handlers.orderedStream().toList(), failureHandler);
    }

    @Bean
    @ConditionalOnMissingBean(name = "authenticationFilterRegistration")
    public FilterRegistrationBean<Filter> authenticationFilterRegistration(AuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setName(AuthenticationFilter.class.getName());
        registration.setOrder(AuthorizationFilter.REGISTRATION_ORDER_PREVIOUS);
        return registration;
    }
}
