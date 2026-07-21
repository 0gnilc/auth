package com.gnilc.system.config;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 系统模块配置。
 */
@Configuration
@MapperScan({"com.gnilc.system.admin.dao", "com.gnilc.system.i18n.dao"})
@Import({RestExceptionHandlingConfiguration.class, I18nMessageService.class})
public class SystemConfiguration {

}
