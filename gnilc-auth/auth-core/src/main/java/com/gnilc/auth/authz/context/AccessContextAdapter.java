package com.gnilc.auth.authz.context;

/**
 * 将执行环境对象翻译成访问上下文。
 * <p>
 * Adapter 是执行环境进入授权核心的主 seam，负责隔离 Web、消息、任务等环境细节，并组合访问环境、
 * 访问身份、访问目标和补充属性，输出完整 {@link AccessContext}。授权核心只接收访问上下文，
 * 不接收原始执行环境对象。
 * <p>
 * {@link AccessEnvironmentResolver}、{@link AccessIdentityResolver} 与 {@link AccessTargetResolver} 可以作为
 * adapter implementation 内部的 helper seam 使用，但不是构造访问上下文的强制依赖。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessContextAdapter<T> {
    /**
     * 翻译执行环境对象。
     *
     * @param source 执行环境对象
     * @return 访问上下文
     */
    AccessContext adapt(T source);
}
