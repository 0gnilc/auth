package com.gnilc.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Gnilc Auth 启动入口。 */
@SpringBootApplication(scanBasePackages = "com.gnilc.bootstrap.inspector")
@MapperScan({"com.gnilc.core.admin.dao", "com.gnilc.core.i18n.dao"})
public class AuthBootApplication {
    /** 启动 Gnilc Auth。 */
    public static void main(String[] args) {
        SpringApplication.run(AuthBootApplication.class, args);
    }
}
