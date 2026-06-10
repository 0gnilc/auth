package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.context.AccessIdentityResolver;
import com.gnilc.authz.rbac.context.AccessIdentityResolverDelegate;
import com.gnilc.authz.rbac.context.CompositeAccessIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * RBAC 访问身份解析配置。
 * <p>
 * RBAC 启用时使用组合解析器接入多个认证来源，并统一输出全局用户 ID。
 */
@Configuration(proxyBeanMethods = false)
public class RbacAccessIdentityResolverConfiguration {
    private static final String RBAC_ACCESS_IDENTITY_RESOLVER_BEAN_NAME = "rbacAccessIdentityResolver";
    private static final String SERVLET_ACCESS_IDENTITY_RESOLVER_BEAN_NAME = "servletAccessIdentityResolver";
    private static final Set<String> DEFAULT_ACCESS_IDENTITY_RESOLVER_BEAN_NAMES = Set.of(
            RBAC_ACCESS_IDENTITY_RESOLVER_BEAN_NAME,
            SERVLET_ACCESS_IDENTITY_RESOLVER_BEAN_NAME
    );

    /**
     * 注册 Resolver Bean 定义协调器。
     * <p>
     * core Web 配置会注册匿名的 {@code servletAccessIdentityResolver} 作为默认实现；RBAC 启用时，
     * 应使用 {@code rbacAccessIdentityResolver} 作为默认身份解析器。但如果应用显式提供了完整的
     * {@link AccessIdentityResolver}，则应用实现应优先，并移除 RBAC 与 core 的默认 Resolver，避免注入歧义。
     *
     * @return Resolver Bean 定义协调器
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor rbacServletAccessIdentityResolverOverridePostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
                // 配置类处理过程中 BeanDefinition 尚未完全展开，Resolver 定义统一在 beanFactory 阶段协调。
            }

            @Override
            public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
                    return;
                }
                if (hasCustomAccessIdentityResolver(beanFactory)) {
                    DEFAULT_ACCESS_IDENTITY_RESOLVER_BEAN_NAMES.forEach(beanName -> removeIfExists(registry, beanName));
                }
            }
        };
    }

    /**
     * 创建 RBAC 组合访问身份解析器。
     * <p>
     * Spring 会按照 {@code @Order}/{@code Ordered} 对委托列表排序；每个委托负责一种认证来源，
     * 最终必须输出 RBAC 可消费的全局数字 {@code user_id}。
     *
     * @param delegates 访问身份解析委托列表
     * @return RBAC 访问身份解析器
     */
    @Bean
    @Primary
    public AccessIdentityResolver<HttpServletRequest> rbacAccessIdentityResolver(List<AccessIdentityResolverDelegate<HttpServletRequest>> delegates) {
        return new CompositeAccessIdentityResolver<>(delegates);
    }

    /**
     * 判断应用是否显式提供了完整 Resolver。
     */
    private static boolean hasCustomAccessIdentityResolver(ConfigurableListableBeanFactory beanFactory) {
        return Arrays.stream(beanFactory.getBeanNamesForType(AccessIdentityResolver.class, false, false))
                .anyMatch(name -> !DEFAULT_ACCESS_IDENTITY_RESOLVER_BEAN_NAMES.contains(name));
    }

    /**
     * 按名称移除默认 Resolver Bean 定义。
     */
    private static void removeIfExists(BeanDefinitionRegistry registry, String beanName) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
    }
}
