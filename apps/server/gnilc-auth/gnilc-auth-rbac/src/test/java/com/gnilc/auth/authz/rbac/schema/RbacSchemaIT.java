package com.gnilc.auth.authz.rbac.schema;

import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.test.container.MySqlContainerContextInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(
        classes = RbacTestApplication.class,
        initializers = MySqlContainerContextInitializer.class)
class RbacSchemaIT {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    az_role_menu,
                    az_role_permission,
                    az_user_role,
                    az_menu,
                    az_permission,
                    az_user,
                    az_role
                """);
    }

    @Test
    void rbacSchemaCreatesAllCurrentTables() {
        runScript();

        assertThat(tableNames()).contains(
                "az_role",
                "az_permission",
                "az_menu",
                "az_user",
                "az_user_role",
                "az_role_permission",
                "az_role_menu");
    }

    @Test
    void rbacSchemaCanRunRepeatedly() {
        runScript();
        runScript();

        assertThat(tableNames()).contains(
                "az_role",
                "az_permission",
                "az_menu",
                "az_user",
                "az_user_role",
                "az_role_permission",
                "az_role_menu");
    }

    private java.util.List<String> tableNames() {
        return jdbc.queryForList("""
                select table_name
                  from information_schema.tables
                 where table_schema = database()
                """, String.class);
    }

    private void runScript() {
        new ResourceDatabasePopulator(new ClassPathResource("sql/schema/01_rbac.sql"))
                .execute(dataSource);
    }
}
