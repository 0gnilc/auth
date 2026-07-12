package com.gnilc.auth.authn.servlet.handler;

import com.gnilc.auth.authn.handler.AuthenticationHandler;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;

/**
 * Servlet 认证处理器。
 * <p>
 * 该接口是认证抽象在 Servlet 运行环境下的类型收口；具体认证规则仍由实现类提供。
 */
public interface ServletAuthenticationHandler extends AuthenticationHandler<ServletAuthenticationContext> {
}
