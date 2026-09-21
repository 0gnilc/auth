package com.gnilc.bootstrap;

import com.gnilc.bootstrap.support.BootstrapContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.CountDownLatch;

/** 仅在 E2E 启动器显式启用时，为 Playwright 保持完整应用及隔离测试存储的生命周期。 */
@SpringBootTest(
        classes = AuthBootApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
@EnabledIfEnvironmentVariable(named = "RUN_E2E_SERVER", matches = "true")
class E2eServerIT {
    /** 等待启动器随浏览器测试进程结束本 JVM；此等待不是业务断言，也不是普通单测超时。 */
    @Test
    void serveUntilThePlaywrightProcessStops() throws InterruptedException {
        new CountDownLatch(1).await();
    }
}
