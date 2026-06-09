package com.gnilc.authz.example;

import com.gnilc.authz.rbac.annotation.EnableWebRbacAccessControl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableWebRbacAccessControl
@SpringBootApplication
public class RbacExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RbacExampleApplication.class, args);
    }
}
