package com.gnilc.bootstrap;

import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import com.gnilc.bootstrap.support.BootstrapContainerContextInitializer;
import com.gnilc.bootstrap.support.BootstrapTestConfiguration;
import com.gnilc.system.admin.service.AdminService;
import com.gnilc.test.annotation.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@SpringBootTest(classes = AccessControlApplication.class)
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
@Import(BootstrapTestConfiguration.class)
class ApplicationContextIT {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void loadsAutoConfiguredApplicationAgainstRealInfrastructure() {
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        redisTemplate.opsForValue().set("context-it", "ready");
        assertThat(redisTemplate.opsForValue().get("context-it")).isEqualTo("ready");
        redisTemplate.delete("context-it");

        assertThat(applicationContext.getBean(AdminService.class)).isNotNull();
        Map<String, FilterRegistrationBean> registrations = applicationContext.getBeansOfType(FilterRegistrationBean.class);
        int authenticationOrder = registrations.values().stream()
                .filter(bean -> bean.getFilter() instanceof ServletAuthenticationFilter)
                .findFirst().orElseThrow().getOrder();
        int authorizationOrder = registrations.values().stream()
                .filter(bean -> bean.getFilter() instanceof ServletAuthorizationFilter)
                .findFirst().orElseThrow().getOrder();
        assertThat(authenticationOrder).isLessThan(authorizationOrder);
    }
}
