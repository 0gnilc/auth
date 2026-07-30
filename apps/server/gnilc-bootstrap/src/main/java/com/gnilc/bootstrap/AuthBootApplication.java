package com.gnilc.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gnilc.bootstrap")
public class AuthBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthBootApplication.class, args);
    }
}
