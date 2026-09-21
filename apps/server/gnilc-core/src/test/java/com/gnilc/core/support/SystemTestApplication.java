package com.gnilc.core.support;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
@AutoConfigurationPackage(basePackages = "com.gnilc.core")
public class SystemTestApplication {
}
