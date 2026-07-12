package com.gnilc.test.cleanup;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * 在 Spring 测试方法前恢复应用基线，并在方法后执行兜底清理。
 */
public final class TestDataResetListener extends AbstractTestExecutionListener {
    /** {@inheritDoc} */
    @Override
    public void beforeTestMethod(@NotNull TestContext testContext) {
        manager(testContext).resetToBaseline();
    }

    /** {@inheritDoc} */
    @Override
    public void afterTestMethod(@NotNull TestContext testContext) {
        manager(testContext).cleanAfterTest();
    }

    private TestDataResetManager manager(TestContext context) {
        return context.getApplicationContext().getBean(TestDataResetManager.class);
    }
}
