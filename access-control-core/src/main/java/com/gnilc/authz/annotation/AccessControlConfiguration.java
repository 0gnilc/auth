package com.gnilc.authz.annotation;


import com.gnilc.authz.decision.AccessDecision;
import com.gnilc.authz.decision.AffirmativeAccessDecision;
import com.gnilc.authz.denied.AccessDenied;
import com.gnilc.authz.provider.DelegatingResourcePermissionsProvider;
import com.gnilc.authz.provider.DelegatingSubjectPermissionsProvider;
import com.gnilc.authz.provider.ResourcePermissionsProvider;
import com.gnilc.authz.provider.SubjectPermissionsProvider;
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
public class AccessControlConfiguration {

    @Bean
    @ConditionalOnMissingBean(AccessDecision.class)
    public AccessDecision accessDecision(@Qualifier(DelegatingSubjectPermissionsProvider.BEAN_NAME) DelegatingSubjectPermissionsProvider sp,
                                         @Qualifier(DelegatingResourcePermissionsProvider.BEAN_NAME) DelegatingResourcePermissionsProvider rp) {
        return new AffirmativeAccessDecision(sp, rp);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDenied.class)
    public AccessDenied accessDenied() {
        return resource -> {
            // Do not handle
        };
    }

    @Bean(name = DelegatingSubjectPermissionsProvider.BEAN_NAME)
    @Lazy
    public DelegatingSubjectPermissionsProvider delegatingVisitorOwnedPermissionsProvider(Set<SubjectPermissionsProvider> sps) {
        return new DelegatingSubjectPermissionsProvider(sps);
    }

    @Bean(name = DelegatingResourcePermissionsProvider.BEAN_NAME)
    @Lazy
    public DelegatingResourcePermissionsProvider delegatingTargetAccessiblePermissionsProvider(Set<ResourcePermissionsProvider> rps) {
        return new DelegatingResourcePermissionsProvider(rps);
    }
}
