package com.gnilc.system.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.gnilc.auth.authn.servlet.config.ServletAuthenticationAutoConfiguration;
import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.rbac.config.ServletRbacAuthorizationAutoConfiguration;
import com.gnilc.auth.authz.servlet.config.ServletAuthorizationAutoConfiguration;
import com.gnilc.common.config.LongNumberJacksonConfiguration;
import com.gnilc.common.config.MyMetaObjectHandler;
import com.gnilc.common.config.MybatisPlusConfiguration;
import com.gnilc.common.config.ServletCorsConfiguration;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(before = {
        ServletAuthenticationAutoConfiguration.class,
        AuthorizationAutoConfiguration.class,
        ServletAuthorizationAutoConfiguration.class
}, after = {
        ServletRbacAuthorizationAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        RedisAutoConfiguration.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SqlSessionFactory.class, StringRedisTemplate.class})
@ComponentScan(basePackages = "com.gnilc.system",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class))
@MapperScan({"com.gnilc.system.admin.dao", "com.gnilc.system.i18n.dao"})
@Import({
        LongNumberJacksonConfiguration.class,
        MyMetaObjectHandler.class,
        MybatisPlusConfiguration.class,
        ServletCorsConfiguration.class,
        RestExceptionHandlingConfiguration.class,
        I18nMessageService.class
})
public class SystemAutoConfiguration {

}
