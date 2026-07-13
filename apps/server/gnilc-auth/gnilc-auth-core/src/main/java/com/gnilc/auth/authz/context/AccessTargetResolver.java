package com.gnilc.auth.authz.context;

/**
 * 从执行环境对象解析访问目标。
 * <p>
 * Resolver 是 adapter implementation 内部的访问目标提取辅助 seam，只提取目标事实，
 * 不负责构造完整访问上下文。新执行环境接入应优先实现 {@link AccessContextAdapter}，确保访问环境、
 * 访问身份和访问目标一起进入授权核心。
 * <p>
 * 本 seam 与 {@link AccessEnvironmentResolver}、{@link AccessIdentityResolver} 对称，三者可以由 adapter 组合使用，
 * 但不是 adapter 的强制依赖。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessTargetResolver<T> {
    /**
     * 解析访问目标。
     *
     * @param source 执行环境对象
     * @return 访问目标
     */
    AccessTarget resolve(T source);
}
