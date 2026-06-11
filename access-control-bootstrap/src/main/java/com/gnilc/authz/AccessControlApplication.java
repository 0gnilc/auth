package com.gnilc.authz;

import com.gnilc.authz.rbac.annotation.EnableWebRbacAuthorization;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableWebRbacAuthorization
@SpringBootApplication
public class AccessControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessControlApplication.class, args);
    }
}
