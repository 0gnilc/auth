package com.gnilc.auth.bootstrap;

import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.system.admin.service.AdminService;
import com.gnilc.auth.system.session.AdminSessionManager;
import com.gnilc.auth.system.session.AdminSessionRedisCommands;
import com.gnilc.auth.test.annotation.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class BootstrapContextIT {
    @Autowired
    private Environment environment;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AdminService adminService;
    @Autowired
    private AdminSessionManager adminSessionManager;
    @Autowired
    private AdminSessionRedisCommands adminSessionRedisCommands;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AccessDecision accessDecision;
    @Autowired
    private AccessDenied accessDenied;

    // TestCaseId: IT-BOOTSTRAP-001
    @Test
    void contextLoadsWithApplicationTestConfiguration() {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("gnilc.auth.integration-test.config-source")).isEqualTo("application-test");

        assertThat(dataSource).isNotNull();
        assertThat(redisTemplate).isNotNull();
        assertThat(adminService).isNotNull();
        assertThat(adminSessionManager).isNotNull();
        assertThat(adminSessionRedisCommands).isNotNull();
        assertThat(permissionService).isNotNull();
        assertThat(roleService).isNotNull();
        assertThat(accessDecision).isNotNull();
        assertThat(accessDenied).isNotNull();

        assertThat(applicationContext.getBean(DataSource.class)).isSameAs(dataSource);
        assertThat(applicationContext.getBean(StringRedisTemplate.class)).isSameAs(redisTemplate);
    }
}
