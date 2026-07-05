package com.gnilc.auth.authz.rbac.config;

import com.gnilc.auth.authz.rbac.provider.RbacGrantedPermissionsProvider;
import com.gnilc.auth.authz.rbac.provider.RbacRequiredPermissionsProvider;
import com.gnilc.auth.authz.rbac.provider.cache.LocalPermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheLoader;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ServletRbacAuthorizationAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServletRbacAuthorizationAutoConfiguration.class))
            .withPropertyValues("spring.main.lazy-initialization=true")
            .withBean(SqlSessionFactory.class, this::sqlSessionFactory);

    // Spring Boot 3 SPI 应导入 RBAC Servlet 自动配置。
    // TestCaseId: RBAC-CONFIG-004
    @Test
    void rbacAutoConfigurationImportsContainServletRbacConfiguration() throws IOException {
        String imports = autoConfigurationImports();

        assertThat(imports).contains("com.gnilc.auth.authz.rbac.config.ServletRbacAuthorizationAutoConfiguration");
    }

    // 非 Servlet 环境不启用 RBAC Servlet 自动配置。
    // TestCaseId: RBAC-CONFIG-005
    @Test
    void backsOffInNonServletContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServletRbacAuthorizationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RbacGrantedPermissionsProvider.class);
                    assertThat(context).doesNotHaveBean(RbacRequiredPermissionsProvider.class);
                    assertThat(context).doesNotHaveBean(PermissionCache.class);
                });
    }

    // Servlet + MyBatis 环境应扫描 RBAC service/provider 并显式注册单个默认 PermissionCache。
    // TestCaseId: RBAC-CONFIG-006
    @Test
    void registersRbacServicesProvidersAndSingleDefaultPermissionCacheInServletContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RoleService.class);
            assertThat(context).hasSingleBean(PermissionService.class);
            assertThat(context).hasSingleBean(MenuService.class);
            assertThat(context).hasSingleBean(UserService.class);
            assertThat(context).hasSingleBean(UserRoleService.class);
            assertThat(context).hasSingleBean(RbacGrantedPermissionsProvider.class);
            assertThat(context).hasSingleBean(RbacRequiredPermissionsProvider.class);
            assertThat(context).hasSingleBean(PermissionCache.class);
            assertThat(context.getBean(PermissionCache.class)).isInstanceOf(LocalPermissionCache.class);
        });
    }

    // 应用提供 PermissionCache 时，RBAC 默认本地缓存应让位。
    // TestCaseId: RBAC-CONFIG-007
    @Test
    void keepsApplicationProvidedPermissionCache() {
        contextRunner.withUserConfiguration(CustomPermissionCacheConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PermissionCache.class);
                    assertThat(context.getBean(PermissionCache.class)).isSameAs(context.getBean("customPermissionCache"));
                });
    }

    private String autoConfigurationImports() throws IOException {
        StringBuilder imports = new StringBuilder();
        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            imports.append(new String(resource.openStream().readAllBytes(), StandardCharsets.UTF_8)).append('\n');
        }
        return imports.toString();
    }

    private SqlSessionFactory sqlSessionFactory() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), mock(DataSource.class)));
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPermissionCacheConfiguration {
        @Bean
        @Primary
        PermissionCache customPermissionCache() {
            return new PermissionCache() {
                @Override
                public List<com.gnilc.auth.authz.rbac.provider.TargetPermission> loadTargetPermissions() {
                    return List.of();
                }

                @Override
                public void resetTargetPermissions() {
                }

                @Override
                public List<com.gnilc.auth.authz.provider.Permission> loadUserPermissions(Long userId) {
                    return List.of();
                }

                @Override
                public void resetUserPermissions(Long userId) {
                }

                @Override
                public List<com.gnilc.auth.authz.provider.Permission> loadPublicAccessPermissions() {
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
