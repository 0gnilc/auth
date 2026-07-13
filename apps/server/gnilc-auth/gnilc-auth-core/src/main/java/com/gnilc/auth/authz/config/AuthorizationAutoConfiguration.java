package com.gnilc.auth.authz.config;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.decision.AffirmativeAccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.auth.authz.denied.DefaultAccessDenied;
import com.gnilc.auth.authz.provider.DelegatingGrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.DelegatingRequiredPermissionsProvider;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

import java.util.Set;

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationAutoConfiguration {

    @Bean(name = DelegatingGrantedPermissionsProvider.BEAN_NAME)
    @Lazy
    @ConditionalOnBean(GrantedPermissionsProvider.class)
    @ConditionalOnMissingBean(name = DelegatingGrantedPermissionsProvider.BEAN_NAME)
    public DelegatingGrantedPermissionsProvider delegatingGrantedPermissionsProvider(Set<GrantedPermissionsProvider> providers) {
        return new DelegatingGrantedPermissionsProvider(providers);
    }

    @Bean(name = DelegatingRequiredPermissionsProvider.BEAN_NAME)
    @Lazy
    @ConditionalOnBean(RequiredPermissionsProvider.class)
    @ConditionalOnMissingBean(name = DelegatingRequiredPermissionsProvider.BEAN_NAME)
    public DelegatingRequiredPermissionsProvider delegatingRequiredPermissionsProvider(Set<RequiredPermissionsProvider> providers) {
        return new DelegatingRequiredPermissionsProvider(providers);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDecision.class)
    @ConditionalOnBean(name = {
            DelegatingGrantedPermissionsProvider.BEAN_NAME,
            DelegatingRequiredPermissionsProvider.BEAN_NAME
    })
    public AccessDecision accessDecision(@Qualifier(DelegatingGrantedPermissionsProvider.BEAN_NAME) GrantedPermissionsProvider granted,
                                         @Qualifier(DelegatingRequiredPermissionsProvider.BEAN_NAME) RequiredPermissionsProvider required) {
        return new AffirmativeAccessDecision(granted, required);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDenied.class)
    public AccessDenied accessDenied(ObjectProvider<AccessDeniedHandler> handlers) {
        return new DefaultAccessDenied(handlers.orderedStream().toList());
    }
}
