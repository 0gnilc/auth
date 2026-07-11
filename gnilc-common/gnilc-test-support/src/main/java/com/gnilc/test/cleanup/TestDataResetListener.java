package com.gnilc.test.cleanup;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * 根据 {@link com.gnilc.test.annotation.CleanTestData} 在测试方法前后执行数据隔离。
 */
public class TestDataResetListener extends AbstractTestExecutionListener {
    /**
     * 在测试方法执行前按声明的模式重置数据，并在需要时恢复基线数据。
     */
    @Override
    public void beforeTestMethod(TestContext testContext) {
        reset(testContext);
    }

    /**
     * 在测试方法执行后再次清理数据，但不恢复基线数据。
     */
    @Override
    public void afterTestMethod(TestContext testContext) {
        TestDataResetManager manager = testContext.getApplicationContext().getBean(TestDataResetManager.class);
        manager.cleanupAfter(TestDataResetManager.mode(testContext.getTestClass(), testContext.getTestMethod()));
    }

    private void reset(TestContext testContext) {
        TestDataResetManager manager = testContext.getApplicationContext().getBean(TestDataResetManager.class);
        manager.reset(TestDataResetManager.mode(testContext.getTestClass(), testContext.getTestMethod()));
    }
}
