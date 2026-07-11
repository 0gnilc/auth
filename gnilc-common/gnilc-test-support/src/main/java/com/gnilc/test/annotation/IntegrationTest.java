package com.gnilc.test.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记由 Spring TestContext 驱动的集成测试。
 * <p>
 * 该注解只注册 Spring JUnit 扩展并启用 {@code test} Profile，
 * 不会隐式创建 {@code @SpringBootTest} 上下文或启动测试容器。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
