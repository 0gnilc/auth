package com.gnilc.auth.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CoreAutoConfigurationImportsTest {

    // Spring Boot 3 SPI 应导入 core 模块所有自动配置。
    // TestCaseId: CORE-CONFIG-001
    @Test
    void coreAutoConfigurationImportsContainAuthnAndAuthzAutoConfigurations() throws IOException {
        String imports = new String(getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(imports).contains("com.gnilc.auth.authn.servlet.config.ServletAuthenticationAutoConfiguration");
        assertThat(imports).contains("com.gnilc.auth.authz.config.AuthorizationAutoConfiguration");
        assertThat(imports).contains("com.gnilc.auth.authz.servlet.config.ServletAuthorizationAutoConfiguration");
    }
}
