package com.gnilc.test.annotation;

import com.gnilc.test.cleanup.TestCleanupConfiguration;
import com.gnilc.test.cleanup.TestDataResetListener;
import com.gnilc.test.container.FullStackContainerContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记运行在随机端口、使用真实 MySQL/Redis 且每个方法重置数据基线的接口测试。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = FullStackContainerContextInitializer.class)
@Import(TestCleanupConfiguration.class)
@TestExecutionListeners(
        listeners = TestDataResetListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface ApiTest {
}
