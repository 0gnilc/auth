package com.gnilc.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class MyMetaObjectHandlerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HandlerImportConfiguration.class);

    @Test
    void registersTheDefaultHandler() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MetaObjectHandler.class);
            assertThat(context.getBean(MetaObjectHandler.class))
                    .isInstanceOf(MyMetaObjectHandler.class);
        });
    }

    @Test
    void backsOffWhenAHandlerAlreadyExists() {
        MetaObjectHandler customHandler = new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
            }

            @Override
            public void updateFill(MetaObject metaObject) {
            }
        };

        contextRunner
                .withBean("customMetaObjectHandler", MetaObjectHandler.class, () -> customHandler)
                .run(context -> {
                    assertThat(context).hasSingleBean(MetaObjectHandler.class);
                    assertThat(context.getBean(MetaObjectHandler.class)).isSameAs(customHandler);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MyMetaObjectHandler.class)
    static class HandlerImportConfiguration {
    }
}
