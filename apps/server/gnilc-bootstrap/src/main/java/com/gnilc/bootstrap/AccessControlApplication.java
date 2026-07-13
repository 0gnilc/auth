package com.gnilc.bootstrap;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.gnilc.bootstrap")
@Import(RestExceptionHandlingConfiguration.class)
public class AccessControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessControlApplication.class, args);
    }
}
