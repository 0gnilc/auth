package com.gnilc.system.config;

import com.gnilc.system.admin.controller.AdminController;
import com.gnilc.system.admin.service.AdminService;
import com.gnilc.system.session.AdminSessionManager;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SystemAutoConfiguration.class));

    @Test
    void publishesExactlyTheSystemAutoConfiguration() throws IOException {
        ClassPathResource imports = new ClassPathResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        assertThat(imports.getContentAsString(StandardCharsets.UTF_8).lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList())
                .containsExactly(SystemAutoConfiguration.class.getName());
    }

    @Test
    void backsOffOutsideServletApplications() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AdminController.class);
            assertThat(context).doesNotHaveBean(AdminService.class);
            assertThat(context).doesNotHaveBean(AdminSessionManager.class);
        });
    }

    @Test
    void controlConfigurationScansOnlyTheSystemAdminDaoPackage() {
        MapperScan mapperScan = SystemControlConfiguration.class.getAnnotation(MapperScan.class);

        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.value()).containsExactly("com.gnilc.system.admin.dao");
    }
}
