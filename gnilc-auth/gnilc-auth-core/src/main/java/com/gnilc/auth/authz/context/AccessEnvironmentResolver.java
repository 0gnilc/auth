package com.gnilc.auth.authz.context;

/**
 * 从执行环境对象解析访问环境。
 * <p>
 * Resolver 是 adapter implementation 内部的访问环境提取辅助 seam，只提取环境事实，
 * 不负责构造完整访问上下文。新执行环境接入应优先实现 {@link AccessContextAdapter}，确保访问环境、
 * 访问身份和访问目标一起进入授权核心。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessEnvironmentResolver<T> {
    /**
     * 解析访问环境。
     *
     * @param source 执行环境对象
     * @return 访问环境
     */
    AccessEnvironment resolve(T source);
}
