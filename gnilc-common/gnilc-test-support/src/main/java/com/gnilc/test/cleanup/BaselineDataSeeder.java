package com.gnilc.test.cleanup;

/**
 * 在数据库完成清理后恢复测试所需基线业务数据的扩展点。
 * <p>
 * 具体数据由所属业务模块提供，本通用模块只负责按顺序调用。
 */
@FunctionalInterface
public interface BaselineDataSeeder {
    /**
     * 向当前测试数据库写入一组基线数据。
     */
    void seed();
}
