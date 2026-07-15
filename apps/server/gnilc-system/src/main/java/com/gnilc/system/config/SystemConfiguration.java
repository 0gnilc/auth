package com.gnilc.system.config;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 系统模块配置。
 */
@Configuration
@MapperScan("com.gnilc.system.admin.dao")
@Import(RestExceptionHandlingConfiguration.class)
public class SystemConfiguration {

}
