package com.gnilc.test.annotation;

import com.gnilc.test.cleanup.CleanupMode;
import com.gnilc.test.cleanup.TestCleanupConfiguration;
import com.gnilc.test.cleanup.TestDataResetListener;
import com.gnilc.test.container.FullStackContainerContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@IntegrationTest
@CleanTestData(CleanupMode.BASELINE_RESET)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = FullStackContainerContextInitializer.class)
@Import(TestCleanupConfiguration.class)
@TestExecutionListeners(value = TestDataResetListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface ApiTest {
}
