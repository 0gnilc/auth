package com.gnilc.core.config;

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
import com.gnilc.core.context.UserContextService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 显式组合 Server 业务与共享基础配置；扫描业务时排除配置类，避免依赖包自行激活另一套基础设施。 */
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
@ComponentScan(basePackages = {
        "com.gnilc.core.admin",
        "com.gnilc.core.auth",
        "com.gnilc.core.authz",
        "com.gnilc.core.i18n",
        "com.gnilc.core.session"
},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class))
@Import({
        LongNumberJacksonConfiguration.class,
        MyMetaObjectHandler.class,
        MybatisPlusConfiguration.class,
        ServletCorsConfiguration.class,
        RestExceptionHandlingConfiguration.class,
        I18nMessageService.class,
        UserContextService.class
})
public class SystemAutoConfiguration {

}
