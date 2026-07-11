package com.gnilc.system.config;

import com.gnilc.system.admin.controller.AdminController;
import com.gnilc.system.admin.service.AdminService;
import com.gnilc.system.session.AdminSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SystemAutoConfiguration.class));

    @Test
    void backsOffOutsideServletApplications() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AdminController.class);
            assertThat(context).doesNotHaveBean(AdminService.class);
            assertThat(context).doesNotHaveBean(AdminSessionManager.class);
        });
    }
}
