package com.gnilc.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

@Configuration(value = "longNumberJacksonConfiguration", proxyBeanMethods = false)
public class LongNumberJacksonConfiguration {

    @Bean("longNumberJacksonCustomizer")
    @ConditionalOnMissingBean(name = "longNumberJacksonCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer longNumberJacksonCustomizer() {
        return builder -> {
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
