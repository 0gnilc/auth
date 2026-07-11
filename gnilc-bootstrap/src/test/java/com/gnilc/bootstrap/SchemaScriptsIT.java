package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.BootstrapContainerContextInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AccessControlApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
@Transactional
class SchemaScriptsIT {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void restoreDeploymentBaseline() {
        new ResourceDatabasePopulator(new ClassPathResource("sql/schema/02-admin.sql"))
                .execute(dataSource);
    }

    @Test
    void deploymentScriptsCreateAllCurrentTablesAndDefaultAdmin() {
        assertThat(jdbc.queryForList("""
                select table_name
                  from information_schema.tables
                 where table_schema = database()
                """, String.class))
                .contains("az_role", "az_permission", "az_menu", "az_user", "az_user_role",
                        "az_role_permission", "az_role_menu", "sys_admin");
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_admin where username = 'admin' and del = 0", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*)
                  from az_user_role ur
                  join sys_admin a on a.user_id = ur.user_id
                  join az_role r on r.id = ur.role_id
                 where a.username = 'admin' and r.code = 'admin'
                """, Integer.class)).isEqualTo(1);
    }
}
