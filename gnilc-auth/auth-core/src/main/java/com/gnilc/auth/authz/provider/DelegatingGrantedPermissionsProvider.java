package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.google.common.base.Preconditions;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 聚合多个已授予权限提供者。
 * <p>
 * 用于把用户、公开访问、临时授权等来源统一成一组已授予权限。
 */
public class DelegatingGrantedPermissionsProvider implements GrantedPermissionsProvider {
    public static final String BEAN_NAME = "delegatingGrantedPermissionsProvider";

    private final Set<GrantedPermissionsProvider> providers;

    /**
     * 创建聚合已授予权限提供者。
     *
     * @param providers 被聚合的已授予权限提供者
     */
    public DelegatingGrantedPermissionsProvider(Set<GrantedPermissionsProvider> providers) {
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
