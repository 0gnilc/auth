package com.gnilc.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class SharedInfrastructureImportTest {

    @Test
    void repeatedImportsRegisterEachSharedComponentOnce() {
        new ApplicationContextRunner()
                .withUserConfiguration(FirstConsumer.class, SecondConsumer.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(MetaObjectHandler.class);
                    assertThat(context)
                            .getBeans(Jackson2ObjectMapperBuilderCustomizer.class)
                            .hasSize(1);
                    assertThat(context.getBeansOfType(FilterRegistrationBean.class))
                            .hasSize(1);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            MybatisPlusConfiguration.class,
            MyMetaObjectHandler.class,
            LongNumberJacksonConfiguration.class,
            ServletCorsConfiguration.class
    })
    static class FirstConsumer {
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            MybatisPlusConfiguration.class,
            MyMetaObjectHandler.class,
            LongNumberJacksonConfiguration.class,
            ServletCorsConfiguration.class
    })
    static class SecondConsumer {
    }
}
