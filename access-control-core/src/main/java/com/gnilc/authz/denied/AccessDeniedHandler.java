package com.gnilc.authz.denied;

import com.gnilc.authz.context.AccessContext;

/**
 * 访问拒绝处理器。
 *
 * @param <T> 执行环境中的拒绝处理上下文类型
 */
public interface AccessDeniedHandler<T> {
    /**
     * 处理访问拒绝。
     *
     * @param context       访问上下文
     * @param deniedContext 执行环境拒绝处理上下文
     */
    void handle(AccessContext context, T deniedContext);
}
