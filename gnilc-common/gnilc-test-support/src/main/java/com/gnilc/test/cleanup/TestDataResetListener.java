package com.gnilc.test.cleanup;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class TestDataResetListener extends AbstractTestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        reset(testContext);
    }

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
