package com.gnilc.auth.authn.servlet.handler;

import com.gnilc.auth.authn.handler.AuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;

/**
 * Servlet 认证失败处理器。
 * <p>
 * 将抽象认证失败结果转换为 Servlet HTTP 响应。
 */
public interface ServletAuthenticationFailureHandler extends AuthenticationFailureHandler<ServletAuthenticationContext> {
}
