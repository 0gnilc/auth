package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.google.common.base.Preconditions;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 聚合多个所需权限提供者。
 * <p>
 * 用于把路由、业务访问点等目标来源统一成一组所需权限。
 */
public class DelegatingRequiredPermissionsProvider implements RequiredPermissionsProvider {
    public static final String BEAN_NAME = "delegatingRequiredPermissionsProvider";

    private final Set<RequiredPermissionsProvider> providers;

    /**
     * 创建聚合所需权限提供者。
     *
     * @param providers 被聚合的所需权限提供者
     */
    public DelegatingRequiredPermissionsProvider(Set<RequiredPermissionsProvider> providers) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(providers), "providers is Empty!");
        this.providers = providers;
    }

    @Override
    public List<Permission> provide(AccessContext context) {
        return providers.stream()
                .filter(provider -> provider.supports(context))
                .flatMap(provider -> Optional.ofNullable(provider.provide(context)).orElse(List.of()).stream())
                .distinct()
                .toList();
    }
}
