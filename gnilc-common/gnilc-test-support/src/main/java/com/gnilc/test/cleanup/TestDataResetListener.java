package com.gnilc.test.cleanup;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class TestDataResetListener extends AbstractTestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        manager(testContext).resetToBaseline();
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        manager(testContext).cleanAfterTest();
    }

    private TestDataResetManager manager(TestContext context) {
        return context.getApplicationContext().getBean(TestDataResetManager.class);
    }
}
