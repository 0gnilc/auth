package com.gnilc.auth.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CoreAutoConfigurationImportsTest {
    private static final String IMPORTS_RESOURCE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    @Test
    void importsExactlyTheCoreAutoConfigurations() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(IMPORTS_RESOURCE)) {
            assertThat(input).as(IMPORTS_RESOURCE).isNotNull();
            String contents = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(contents).isEqualTo("""
                    com.gnilc.auth.authn.servlet.config.ServletAuthenticationAutoConfiguration
                    com.gnilc.auth.authz.config.AuthorizationAutoConfiguration
                    com.gnilc.auth.authz.servlet.config.ServletAuthorizationAutoConfiguration
                    """);
        }
    }
}
