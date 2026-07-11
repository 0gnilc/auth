package com.gnilc.test.annotation;

import com.gnilc.test.cleanup.CleanupMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明测试类或测试方法所需的数据清理方式。
 * <p>
 * 方法上的声明优先于类上的声明；实际清理由
 * {@link com.gnilc.test.cleanup.TestDataResetListener} 执行。
 * 该注解本身只提供元数据，使用时还必须注册上述监听器，
 * 并在 Spring 测试上下文中提供 {@link com.gnilc.test.cleanup.TestDataResetManager} Bean；
 * {@link ApiTest} 已组合这些依赖。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CleanTestData {
    /**
     * 返回当前测试所需的清理模式。
     *
     * @return 清理模式，默认不执行清理
     */
    CleanupMode value() default CleanupMode.NONE;
}
