package com.gnilc.auth.authz.context;

import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 组合访问身份解析器。
 * <p>
 * 该 resolver 定义 {@link AccessIdentityResolverHandler} 的组合规范：按顺序选择第一个支持当前执行环境对象的
 * handler，并原样返回 handler 的身份解析结果；没有 handler 命中时使用 fallback resolver。
 *
 * @param <T> 执行环境对象类型
 */
public class DelegatingAccessIdentityResolver<T> implements AccessIdentityResolver<T> {
    private final List<? extends AccessIdentityResolverHandler<T>> handlers;
    private final AccessIdentityResolver<T> fallbackResolver;

    /**
     * 创建组合访问身份解析器。
     *
     * @param handlers         访问身份解析处理器列表
     * @param fallbackResolver 无 handler 命中时使用的 fallback resolver
     */
    public DelegatingAccessIdentityResolver(List<? extends AccessIdentityResolverHandler<T>> handlers,
                                            AccessIdentityResolver<T> fallbackResolver) {
        this.handlers = CollectionUtils.isEmpty(handlers) ? List.of() : List.copyOf(handlers);
        this.fallbackResolver = Objects.requireNonNull(fallbackResolver, "fallbackResolver must not be null");
    }

    @Override
    public AccessIdentity resolve(T source) {
        for (AccessIdentityResolverHandler<T> handler : handlers) {
            if (handler.supports(source)) {
                return handler.handle(source);
            }
        }
        return fallbackResolver.resolve(source);
    }
}
