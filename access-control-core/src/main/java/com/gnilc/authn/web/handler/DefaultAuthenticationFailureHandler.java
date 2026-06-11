package com.gnilc.authn.web.handler;

import com.gnilc.authn.web.context.ServletAuthenticationContext;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认 Servlet 认证失败处理器。
 */
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static final String DEFAULT_MESSAGE = "authentication failed";

    @Override
    public void handle(ServletAuthenticationContext context, AuthenticationResult result) {
        HttpServletResponse response = context.getResponse();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        try {
            response.getWriter().write(resolveMessage(result));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write authentication failure response", e);
        }
    }

    private String resolveMessage(AuthenticationResult result) {
        if (result == null || result.getReason() == null || result.getReason().isBlank()) {
            return DEFAULT_MESSAGE;
        }
        return result.getReason();
    }
}
