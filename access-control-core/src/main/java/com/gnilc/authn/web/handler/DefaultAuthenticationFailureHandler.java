package com.gnilc.authn.web.handler;

import com.gnilc.authn.web.context.ServletAuthenticationContext;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 默认 Servlet 认证失败处理器。
 */
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void handle(ServletAuthenticationContext context, AuthenticationResult result) {
        context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
