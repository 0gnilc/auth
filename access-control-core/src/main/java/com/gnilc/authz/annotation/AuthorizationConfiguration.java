package com.gnilc.authz.annotation;


import com.gnilc.authz.decision.AccessDecision;
import com.gnilc.authz.decision.AffirmativeAccessDecision;
import com.gnilc.authz.provider.DelegatingGrantedPermissionsProvider;
import com.gnilc.authz.provider.DelegatingRequiredPermissionsProvider;
import com.gnilc.authz.provider.GrantedPermissionsProvider;
import com.gnilc.authz.provider.RequiredPermissionsProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import java.util.Set;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration(proxyBeanMethods = false)
public class AuthorizationConfiguration {

    @Bean
    @ConditionalOnMissingBean(AccessDecision.class)
    public AccessDecision accessDecision(@Qualifier(DelegatingGrantedPermissionsProvider.BEAN_NAME) DelegatingGrantedPermissionsProvider granted,
                                         @Qualifier(DelegatingRequiredPermissionsProvider.BEAN_NAME) DelegatingRequiredPermissionsProvider required) {
        return new AffirmativeAccessDecision(granted, required);
    }

    @Bean(name = DelegatingGrantedPermissionsProvider.BEAN_NAME)
    @Lazy
    public DelegatingGrantedPermissionsProvider delegatingGrantedPermissionsProvider(Set<GrantedPermissionsProvider> providers) {
        return new DelegatingGrantedPermissionsProvider(providers);
    }

    @Bean(name = DelegatingRequiredPermissionsProvider.BEAN_NAME)
    @Lazy
    public DelegatingRequiredPermissionsProvider delegatingRequiredPermissionsProvider(Set<RequiredPermissionsProvider> providers) {
        return new DelegatingRequiredPermissionsProvider(providers);
    }
}
