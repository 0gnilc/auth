package com.gnilc.auth.authn.servlet.handler;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认 Servlet 认证失败处理器。
 */
public class DefaultServletAuthenticationFailureHandler implements ServletAuthenticationFailureHandler {
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
        if (result == null || !StringUtils.hasText(result.getReason())) {
            return DEFAULT_MESSAGE;
        }
        return result.getReason();
    }
}
