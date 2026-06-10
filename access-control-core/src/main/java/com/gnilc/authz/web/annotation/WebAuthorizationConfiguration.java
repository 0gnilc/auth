package com.gnilc.authz.web.annotation;


import com.gnilc.authz.annotation.AuthorizationConfiguration;
import com.gnilc.authz.context.AccessContextAdapter;
import com.gnilc.authz.context.AccessIdentityResolver;
import com.gnilc.authz.decision.AccessDecision;
import com.gnilc.authz.denied.AccessDeniedHandler;
import com.gnilc.authz.web.context.FilterDeniedContext;
import com.gnilc.authz.web.context.ServletAccessContextAdapter;
import com.gnilc.authz.web.context.ServletAccessIdentityResolver;
import com.gnilc.authz.web.filter.AuthorizationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;



@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnClass(DispatcherServlet.class)
@AutoConfigureAfter({DispatcherServletAutoConfiguration.class, AuthorizationConfiguration.class})
public class WebAuthorizationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessIdentityResolver<HttpServletRequest> servletAccessIdentityResolver() {
        return new ServletAccessIdentityResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessContextAdapter<HttpServletRequest> servletAccessContextAdapter(AccessIdentityResolver<HttpServletRequest> accessIdentityResolver) {
        return new ServletAccessContextAdapter(accessIdentityResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler<FilterDeniedContext> accessDeniedHandler() {
        return (context, deniedContext) -> {
            // Do not handle
        };
    }

    @Bean
    public FilterRegistrationBean<Filter> authorizationFilterRegistration(AccessContextAdapter<HttpServletRequest> accessContextAdapter,
                                                                         AccessDecision accessDecision,
                                                                         AccessDeniedHandler<FilterDeniedContext> accessDeniedHandler) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        AuthorizationFilter authorizationFilter = new AuthorizationFilter(accessContextAdapter, accessDecision, accessDeniedHandler);
        registration.setFilter(authorizationFilter);
        registration.addUrlPatterns("/*");
        registration.setName(AuthorizationFilter.class.getName());
        registration.setOrder(AuthorizationFilter.REGISTRATION_ORDER);
        return registration;
    }
}
