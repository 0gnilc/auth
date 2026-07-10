package com.gnilc.system.config;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.system.admin.controller.AdminController;
import com.gnilc.system.admin.service.AdminService;
import com.gnilc.system.auth.AdminSessionAuthenticationHandler;
import com.gnilc.system.auth.DefaultServletAccessDeniedHandler;
import com.gnilc.system.session.AdminSessionManager;
import com.gnilc.system.session.AdminSessionRedisCommands;
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
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SystemAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SystemAutoConfiguration.class))
            .withPropertyValues("spring.main.lazy-initialization=true")
            .withUserConfiguration(InfrastructureConfiguration.class);

    // 非 Servlet 环境不启用 System 自动配置。
    // TestCaseId: SYS-CONFIG-003
    @Test
    void backsOffInNonServletContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SystemAutoConfiguration.class))
                .withUserConfiguration(InfrastructureConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AdminController.class);
                    assertThat(context).doesNotHaveBean(AdminService.class);
                    assertThat(context).doesNotHaveBean(AdminSessionManager.class);
                });
    }

    // Servlet + MyBatis + Redis 环境应通过 System SPI 注册 System 组件。
    // TestCaseId: SYS-CONFIG-004
    @Test
    void registersSystemComponentsInServletInfrastructureContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AdminController.class);
            assertThat(context).hasSingleBean(AdminService.class);
            assertThat(context).hasSingleBean(AdminSessionManager.class);
            assertThat(context).hasSingleBean(AdminSessionRedisCommands.class);
            assertThat(context).hasSingleBean(AdminSessionAuthenticationHandler.class);
            assertThat(context).hasSingleBean(DefaultServletAccessDeniedHandler.class);
            assertThat(context).hasSingleBean(AccessDeniedHandler.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class InfrastructureConfiguration {
        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
            return new SqlSessionFactoryBuilder().build(configuration);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        com.gnilc.auth.authz.rbac.service.RoleService roleService() {
            return mock(com.gnilc.auth.authz.rbac.service.RoleService.class);
        }

        @Bean
        com.gnilc.auth.authz.rbac.service.MenuService menuService() {
            return mock(com.gnilc.auth.authz.rbac.service.MenuService.class);
        }

        @Bean
        com.gnilc.auth.authz.rbac.service.UserService userService() {
            return mock(com.gnilc.auth.authz.rbac.service.UserService.class);
        }

        @Bean
        com.gnilc.auth.authz.rbac.service.UserRoleService userRoleService() {
            return mock(com.gnilc.auth.authz.rbac.service.UserRoleService.class);
        }
    }
}
