package com.gnilc.auth.system.config;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControlConfigurationTest {

    /**
     * 验证系统 Mapper 扫描配置。
     */
    // TestCaseId: SYS-CONFIG-001
    @Test
    void configuresSystemAdminMapperScan() {
        MapperScan mapperScan = SystemControlConfiguration.class.getAnnotation(MapperScan.class);

        assertThat(SystemControlConfiguration.class).hasAnnotation(Configuration.class);
        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.value()).containsExactly("com.gnilc.auth.system.admin.dao");
    }
}
