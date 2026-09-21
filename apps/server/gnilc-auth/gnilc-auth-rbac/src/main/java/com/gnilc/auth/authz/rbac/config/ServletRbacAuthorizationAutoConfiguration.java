package com.gnilc.auth.authz.rbac.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.gnilc.auth.authz.config.AuthorizationAutoConfiguration;
import com.gnilc.auth.authz.rbac.provider.cache.LocalPermissionCacheService;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheLoader;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
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
import org.springframework.context.annotation.*;

/** 为 Servlet RBAC 装配持久化与权限依赖；未提供自定义权限缓存时默认使用本地缓存。 */
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

    @Bean
    @ConditionalOnMissingBean(PermissionCacheService.class)
    public PermissionCacheService permissionCacheService(PermissionCacheLoader cacheLoader) {
        return new LocalPermissionCacheService(cacheLoader);
    }
}
