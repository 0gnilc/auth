package com.gnilc.authz.context;

/**
 * 将执行环境对象翻译成访问上下文。
 * <p>
 * Adapter 负责隔离 Web、消息、任务等环境细节，授权核心只接收 {@link AccessContext}。
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
