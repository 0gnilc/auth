package com.gnilc.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 配置系统模块 Mapper 扫描。
 */
@Configuration
@MapperScan("com.gnilc.system.admin.dao")
public class SystemControlConfiguration {
    
}
