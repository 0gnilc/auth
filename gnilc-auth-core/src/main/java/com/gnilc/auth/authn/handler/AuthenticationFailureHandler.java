package com.gnilc.auth.authn.handler;

import com.gnilc.auth.authn.context.AuthenticationContext;

/**
 * 认证失败处理器。
 * <p>
 * 该抽象只定义失败处理扩展点，具体响应格式由运行环境适配层决定。
 *
 * @param <T> 认证上下文类型
 */
public interface AuthenticationFailureHandler<T extends AuthenticationContext> {
    /**
     * 处理认证失败。
     *
     * @param context 认证上下文
     * @param result  认证失败结果
     */
    void handle(T context, AuthenticationResult result);
}
