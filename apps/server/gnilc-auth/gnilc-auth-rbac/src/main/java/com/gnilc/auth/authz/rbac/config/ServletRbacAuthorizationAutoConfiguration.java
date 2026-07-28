package com.gnilc.auth.authz.rbac.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.rbac.provider.cache.LocalPermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheLoader;
import com.gnilc.auth.authz.rbac.provider.cache.redis.PermissionCacheRedisConfiguration;
import com.gnilc.auth.authz.servlet.config.ServletAuthorizationAutoConfiguration;
import com.gnilc.common.config.LongNumberJacksonConfiguration;
import com.gnilc.common.config.MyMetaObjectHandler;
import com.gnilc.common.config.MybatisPlusConfiguration;
import com.gnilc.common.config.ServletCorsConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = {AuthorizationAutoConfiguration.class, ServletAuthorizationAutoConfiguration.class},
        after = MybatisPlusAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SqlSessionFactory.class)
@ComponentScan(basePackages = "com.gnilc.auth.authz.rbac",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class))
@MapperScan("com.gnilc.auth.authz.rbac.dao")
@Import({
        LongNumberJacksonConfiguration.class,
        MyMetaObjectHandler.class,
        MybatisPlusConfiguration.class,
        ServletCorsConfiguration.class,
        PermissionCacheRedisConfiguration.class,
        I18nMessageService.class
})
public class ServletRbacAuthorizationAutoConfiguration {

    @Bean("localPermissionCache")
    @ConditionalOnMissingBean(PermissionCache.class)
    public LocalPermissionCache localPermissionCache(PermissionCacheLoader permissionCacheLoader) {
        return new LocalPermissionCache(permissionCacheLoader);
    }
}
