package com.gnilc.auth.authn.handler;

import com.gnilc.auth.authn.context.AuthenticationContext;

/**
 * 认证处理器。
 * <p>
 * 一个处理器负责一种认证来源或认证方式。
 *
 * @param <T> 认证上下文类型
 */
public interface AuthenticationHandler<T extends AuthenticationContext> {
    /**
     * 判断当前处理器是否支持该认证上下文。
     *
     * @param context 认证上下文
     * @return 是否支持
     */
    boolean supports(T context);

    /**
     * 执行认证。
     *
     * @param context 认证上下文
     * @return 认证结果
     */
    AuthenticationResult authenticate(T context) throws Exception;
}
