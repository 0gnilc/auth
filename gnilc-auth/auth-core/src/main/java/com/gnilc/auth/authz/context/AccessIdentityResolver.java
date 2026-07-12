package com.gnilc.auth.authz.context;

/**
 * 从执行环境对象解析访问身份。
 * <p>
 * Resolver 是 adapter implementation 内部的身份提取辅助 seam，只提取身份事实，不执行认证流程，
 * 也不负责构造完整访问上下文。新执行环境接入应优先实现 {@link AccessContextAdapter}，确保访问环境、
 * 访问身份和访问目标一起进入授权核心。
 * <p>
 * 本 seam 与 {@link AccessEnvironmentResolver}、{@link AccessTargetResolver} 对称，三者可以由 adapter 组合使用，
 * 但不是 adapter 的强制依赖。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessIdentityResolver<T> {
    /**
     * 解析访问身份。
     *
     * @param source 执行环境对象
     * @return 访问身份
     */
    AccessIdentity resolve(T source);
}
