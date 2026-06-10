package com.gnilc.authz.rbac.context;

import com.gnilc.authz.context.AccessIdentity;

/**
 * RBAC 访问身份解析委托。
 * <p>
 * 一个委托负责适配一种认证来源，但输出的身份标识必须是 RBAC 全局用户 ID。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessIdentityResolverDelegate<T> {
    /**
     * 判断当前委托是否处理该来源。
     *
     * @param source 执行环境对象
     * @return 是否支持当前来源
     */
    boolean supports(T source);

    /**
     * 解析 RBAC 访问身份。
     *
     * @param source 执行环境对象
     * @return 访问身份
     */
    AccessIdentity resolve(T source);
}
