package com.gnilc.auth.authz.context;

import java.util.Objects;

/**
 * 基于 resolver 属性组合访问上下文的抽象适配器。
 * <p>
 * 该 template 固定执行环境对象到 {@link AccessContext} 的构造流程：子类通过构造器提供访问环境、
 * 访问身份和访问目标 resolver 属性，并可按需覆盖补充属性解析；如果某个执行环境需要完全不同的构造流程，
 * 可以直接实现 {@link AccessContextAdapter}。
 *
 * @param <T> 执行环境对象类型
 */
public abstract class AbstractAccessContextAdapter<T> implements AccessContextAdapter<T> {
    private final AccessEnvironmentResolver<T> environmentResolver;
    private final AccessIdentityResolver<T> identityResolver;
    private final AccessTargetResolver<T> targetResolver;

    /**
     * 创建抽象访问上下文适配器。
     *
     * @param environmentResolver 访问环境解析器
     * @param identityResolver    访问身份解析器
     * @param targetResolver      访问目标解析器
     */
    protected AbstractAccessContextAdapter(AccessEnvironmentResolver<T> environmentResolver,
                                           AccessIdentityResolver<T> identityResolver,
                                           AccessTargetResolver<T> targetResolver) {
        this.environmentResolver = Objects.requireNonNull(environmentResolver, "environmentResolver must not be null");
        this.identityResolver = Objects.requireNonNull(identityResolver, "identityResolver must not be null");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver must not be null");
    }

    /**
     * 组合访问环境、访问身份、访问目标和补充属性，构造访问上下文。
     */
    @Override
    public final AccessContext adapt(T source) {
        return new AccessContext(
                environmentResolver.resolve(source),
                identityResolver.resolve(source),
                targetResolver.resolve(source)
        );
    }

}
