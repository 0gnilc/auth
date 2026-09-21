package com.gnilc.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationProfileConfigurationTest {
    private static final Map<String, String> TEST_DATABASE_ENVIRONMENT = Map.of(
            "spring.datasource.url", "SPRING_DATASOURCE_URL",
            "spring.datasource.username", "SPRING_DATASOURCE_USERNAME",
            "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void testDatabaseConfigurationUsesEnvironmentBackedDefaults()
            throws IOException {
        PropertySource<?> test = load("application-test.yml");

        TEST_DATABASE_ENVIRONMENT.forEach((property, environment) ->
                assertEnvironmentBacked(test, property, environment));
        assertThat(String.valueOf(test.getProperty("spring.datasource.password")))
                .contains("dev-placeholder");
        assertThat(String.valueOf(test.getProperty("spring.datasource.url")))
                .contains("/gnilc_auth?")
                .contains("useAffectedRows=true")
                .contains("useUnicode=true")
                .contains("connectionTimeZone=%2B00:00")
                .contains("forceConnectionTimeZoneToSession=true")
                .contains("preserveInstants=true")
                .contains("characterEncoding=utf-8");
    }

    @Test
    void loggingUsesEnvironmentBackedRollingFileLimits() throws IOException {
        PropertySource<?> base = load("application.yml");

        assertThat(base.getProperty("spring.application.name")).isEqualTo("gnilc-auth");
        assertThat(base.getProperty("logging.file.name"))
                .isEqualTo("${LOG_FILE:logs/${spring.application.name:gnilc-auth}.log}");
        assertThat(base.getProperty("logging.logback.rollingpolicy.max-file-size"))
                .isEqualTo("${LOG_MAX_FILE_SIZE:20MB}");
        assertThat(base.getProperty("logging.logback.rollingpolicy.max-history"))
                .isEqualTo("${LOG_MAX_HISTORY:30}");
        assertThat(base.getProperty("logging.logback.rollingpolicy.total-size-cap"))
                .isEqualTo("${LOG_TOTAL_SIZE_CAP:5GB}");
        assertThat(base.getProperty("logging.logback.rollingpolicy.clean-history-on-start"))
                .isEqualTo("${LOG_CLEAN_HISTORY_ON_START:true}");

        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(base);
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
        assertThat(resolver.getProperty("logging.file.name")).isEqualTo("logs/gnilc-auth.log");
    }

    private void assertEnvironmentBacked(
            PropertySource<?> source,
            String property,
            String environment) {
        assertThat(String.valueOf(source.getProperty(property)))
                .as("%s must use %s with a development fallback", property, environment)
                .startsWith("${" + environment + ":")
                .endsWith("}");
    }

    private PropertySource<?> load(String resourceName) throws IOException {
        var sources = loader.load(resourceName, new ClassPathResource(resourceName));
        assertThat(sources).hasSize(1);
        return sources.get(0);
    }
}
