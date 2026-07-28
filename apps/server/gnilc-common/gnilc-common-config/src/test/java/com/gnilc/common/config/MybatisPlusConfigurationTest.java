package com.gnilc.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusConfiguration.class));

    @Test
    void registersOptimisticLockingBeforeMysqlPagination() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);

            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            assertThat(interceptor.getInterceptors())
                    .hasExactlyElementsOfTypes(
                            OptimisticLockerInnerInterceptor.class,
                            PaginationInnerInterceptor.class);
        });
    }

    @Test
    void backsOffWhenAnInterceptorAlreadyExists() {
        MybatisPlusInterceptor customInterceptor = new MybatisPlusInterceptor();

        contextRunner
                .withBean("customMybatisPlusInterceptor", MybatisPlusInterceptor.class,
                        () -> customInterceptor)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class)).isSameAs(customInterceptor);
                });
    }
}
