package com.gnilc.test.cleanup;

/**
 * 由业务测试模块实现，用于在通用清理完成后恢复该模块所需的数据基线。
 */
@FunctionalInterface
public interface BaselineDataSeeder {
    /** 写入一次完整且可重复创建的测试基线。 */
    void seed();
}
