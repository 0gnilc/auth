package com.gnilc.auth.authz.rbac.config;

import com.gnilc.auth.authz.rbac.provider.RbacGrantedPermissionsProvider;
import com.gnilc.auth.authz.rbac.provider.RbacRequiredPermissionsProvider;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.provider.cache.LocalPermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheLoader;
import com.gnilc.auth.authz.provider.Permission;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RbacAutoConfigurationTest {
    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletRbacAuthorizationAutoConfiguration.class))
            .withPropertyValues("spring.main.lazy-initialization=true")
            .withBean(SqlSessionFactory.class, this::sqlSessionFactory);

    @Test
    void registersProvidersAndTheDefaultLocalCacheInAServletApplication() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(RbacGrantedPermissionsProvider.class);
            assertThat(context).hasSingleBean(RbacRequiredPermissionsProvider.class);
            assertThat(context).hasSingleBean(PermissionCache.class);
            assertThat(context.getBean(PermissionCache.class)).isInstanceOf(LocalPermissionCache.class);
        });
    }

    @Test
    void backsOffOutsideServletApplications() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletRbacAuthorizationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RbacGrantedPermissionsProvider.class);
                    assertThat(context).doesNotHaveBean(PermissionCache.class);
                });
    }

    @Test
    void keepsAnApplicationProvidedPermissionCache() {
        webContextRunner.withUserConfiguration(CustomPermissionCacheConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PermissionCache.class);
                    assertThat(context.getBean(PermissionCache.class))
                            .isSameAs(context.getBean("customPermissionCache"));
                });
    }

    private SqlSessionFactory sqlSessionFactory() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setEnvironment(new Environment(
                "test", new JdbcTransactionFactory(), mock(DataSource.class)));
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPermissionCacheConfiguration {
        @Bean
        PermissionCache customPermissionCache() {
            return new PermissionCache() {
                @Override
                public List<TargetPermission> loadTargetPermissions() {
                    return List.of();
                }

                @Override
                public void resetTargetPermissions() {
                }

                @Override
                public List<Permission> loadUserPermissions(Long userId) {
                    return List.of();
                }

                @Override
                public void resetUserPermissions(Long userId) {
                }

                @Override
                public List<Permission> loadPublicAccessPermissions() {
                    return List.of();
                }

                @Override
                public void resetPublicAccessPermissions() {
                }

                @Override
                public void resetAll() {
                }
            };
        }
    }
}
