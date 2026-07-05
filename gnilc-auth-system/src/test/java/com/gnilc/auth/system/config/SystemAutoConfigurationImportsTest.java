package com.gnilc.auth.system.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAutoConfigurationImportsTest {

    // Spring Boot 3 SPI 应导入 System 模块自动配置。
    // TestCaseId: SYS-CONFIG-002
    @Test
    void systemAutoConfigurationImportsContainSystemAutoConfiguration() throws IOException {
        String imports = new String(Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")).readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(imports).contains("com.gnilc.auth.system.config.SystemAutoConfiguration");
    }
}
