package com.gnilc.test.container;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FullStackContainerSupportTest {
    @Test
    void mysqlPropertyRegistrationUsesOwnedEndpointWithoutDocker() {
        Map<String, Object> properties = new LinkedHashMap<>();

        MySqlContainerSupport.applyProperties(properties::put,
                "jdbc:mysql://mysql-host:33060/gnilc_auth_test", "user", "secret",
                "com.mysql.cj.jdbc.Driver", "mysql-host", 33060);

        assertThat(properties)
                .containsEntry("spring.datasource.url",
                        "jdbc:mysql://mysql-host:33060/gnilc_auth_test?useAffectedRows=true")
                .containsEntry("spring.datasource.username", "user")
                .containsEntry("spring.datasource.password", "secret")
                .containsEntry("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver")
                .containsEntry("app.test.cleanup.enabled", true)
                .containsEntry("app.test.container.owned", true)
                .containsEntry("app.test.container.mysql.host", "mysql-host")
                .containsEntry("app.test.container.mysql.port", 33060);
    }

    @Test
    void mysqlPropertyRegistrationAppendsJdbcOptionToExistingQuery() {
        Map<String, Object> properties = new LinkedHashMap<>();

        MySqlContainerSupport.applyProperties(properties::put,
                "jdbc:mysql://mysql-host/db?characterEncoding=UTF-8", "user", "secret",
                "driver", "mysql-host", 33060);

        assertThat(properties.get("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://mysql-host/db?characterEncoding=UTF-8&useAffectedRows=true");
    }

    @Test
    void redisPropertyRegistrationUsesOwnedEndpointWithoutDocker() {
        Map<String, Object> properties = new LinkedHashMap<>();

        RedisContainerSupport.applyProperties(properties::put, "redis-host", 16379);

        assertThat(properties)
                .containsEntry("spring.data.redis.host", "redis-host")
                .containsEntry("spring.data.redis.port", 16379)
                .containsEntry("spring.data.redis.database", 0)
                .containsEntry("app.test.cleanup.enabled", true)
                .containsEntry("app.test.container.owned", true)
                .containsEntry("app.test.container.redis.host", "redis-host")
                .containsEntry("app.test.container.redis.port", 16379);
    }

}
