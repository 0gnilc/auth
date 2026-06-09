package com.gnilc.authz.context;

/**
 * 从执行环境对象解析访问身份。
 * <p>
 * Resolver 只提取身份事实，不执行认证流程。
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
