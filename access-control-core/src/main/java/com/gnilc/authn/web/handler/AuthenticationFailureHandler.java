package com.gnilc.authn.web.handler;

import com.gnilc.authn.web.context.ServletAuthenticationContext;

/**
 * Servlet 认证失败处理器。
 * <p>
 * 认证失败通常返回 401；授权失败仍由授权拒绝处理器负责。
 */
public interface AuthenticationFailureHandler {
    /**
     * 处理认证失败。
     *
     * @param context 认证上下文
     * @param result  认证失败结果
     */
    void handle(ServletAuthenticationContext context, AuthenticationResult result);
}
