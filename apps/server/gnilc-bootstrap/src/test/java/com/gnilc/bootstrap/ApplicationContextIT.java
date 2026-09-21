package com.gnilc.bootstrap;

import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import com.gnilc.core.admin.service.AdminService;
import com.gnilc.bootstrap.support.BootstrapContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AuthBootApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
class ApplicationContextIT {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RequestMappingHandlerMapping requestMappings;

    @Test
    void productionAutoConfigurationsComposeTheCompleteApplication() {
        assertThat(context.getBean(AdminService.class)).isNotNull();
        assertThat(context.getBean(AccessDecision.class)).isNotNull();
        assertThat(context.getBean(ServletAuthenticationFilter.class)).isNotNull();
        assertThat(context.getBean(ServletAuthorizationFilter.class)).isNotNull();
        assertThat(context.getBean(StringRedisTemplate.class)).isNotNull();
    }

    @Test
    void migrationHistoryPreservesTheFirstSuccessfulExecution() throws IOException {
        jdbc.execute(new ClassPathResource("sql/migrations/history.sql")
                .getContentAsString(StandardCharsets.UTF_8));
        String migrationId = "test-baseline";
        try {
            jdbc.update("INSERT INTO sys_schema_history (migration_id, checksum_sha256, product_version, git_revision, applied_by) VALUES (?, ?, ?, ?, ?)",
                    migrationId, "a".repeat(64), "1.0.0-rc.1", "b".repeat(40), "integration-test");
            assertThatThrownBy(() -> jdbc.update("INSERT INTO sys_schema_history (migration_id, checksum_sha256, product_version, git_revision, applied_by) VALUES (?, ?, ?, ?, ?)",
                    migrationId, "c".repeat(64), "1.0.0", "d".repeat(40), "integration-test"))
                    .isInstanceOf(DuplicateKeyException.class);
            assertThat(jdbc.queryForObject("SELECT checksum_sha256 FROM sys_schema_history WHERE migration_id = ?", String.class, migrationId))
                    .isEqualTo("a".repeat(64));
            assertThat(jdbc.queryForObject("SELECT applied_at FROM sys_schema_history WHERE migration_id = ?", java.sql.Timestamp.class, migrationId))
                    .isNotNull();
        } finally {
            jdbc.update("DELETE FROM sys_schema_history WHERE migration_id = ?", migrationId);
        }
    }

    @Test
    void deploymentSqlRegistersEveryApplicationEndpointPermission() {
        Set<String> endpointPermissions = new TreeSet<>();
        requestMappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("com.gnilc.")) {
                return;
            }

            Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
            assertThat(methods)
                    .as("HTTP methods for %s", handler)
                    .isNotEmpty();
            for (String pattern : mapping.getPatternValues()) {
                for (RequestMethod method : methods) {
                    endpointPermissions.add(method.name() + ":" + pattern);
                }
            }
        });

        Set<String> deployedPermissions = new TreeSet<>(jdbc.queryForList("""
                SELECT code
                  FROM az_permission
                 WHERE del = 0
                   AND code <> '*:/error'
                """, String.class));

        assertThat(deployedPermissions).containsExactlyElementsOf(endpointPermissions);
    }
}
