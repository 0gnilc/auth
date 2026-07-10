package com.gnilc.bootstrap.support;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import com.gnilc.test.cleanup.TestCleanupConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestCleanupConfiguration.class)
public class BootstrapTestConfiguration {
    @Bean
    BaselineDataSeeder appBaselineDataSeeder(JdbcTemplate jdbcTemplate, PermissionCache permissionCache) {
        return new AppBaselineDataSeeder(jdbcTemplate, permissionCache);
    }
}
