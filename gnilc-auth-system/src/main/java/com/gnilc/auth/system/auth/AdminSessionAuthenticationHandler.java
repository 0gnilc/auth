package com.gnilc.auth.system.auth;

import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.system.session.AdminSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 认证后台管理员 Bearer 访问令牌。
 */
@Component
public class AdminSessionAuthenticationHandler implements ServletAuthenticationHandler {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Pattern BEARER_VALUE_PATTERN = Pattern.compile("^Bearer\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("^Bearer\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    private final AdminSessionManager sessionManager;

    public AdminSessionAuthenticationHandler(AdminSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 判断是否为后台管理员 Bearer 请求。
     */
    @Override
    public boolean supports(ServletAuthenticationContext context) {
        String credentials = resolveBearerCredentials(context.getRequest());
        return credentials != null && sessionManager.supportsAccessToken(credentials.trim());
    }

    /**
     * 校验访问令牌并生成 Principal。
     */
    @Override
    public AuthenticationResult authenticate(ServletAuthenticationContext context) {
        String accessToken = resolveBearerToken(context.getRequest());
        if (accessToken == null) {
            return AuthenticationResult.failed("invalid access token");
        }
        Long userId = sessionManager.validateAccessToken(accessToken);
        if (userId == null) {
            return AuthenticationResult.failed("invalid access token");
        }
        return AuthenticationResult.authenticated(DefaultAccessPrincipal.of(userId));
    }

    /**
     * 解析 Bearer 凭证。
     */
    private String resolveBearerCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null) {
            return null;
        }
        Matcher matcher = BEARER_VALUE_PATTERN.matcher(authorization);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * 解析 Bearer access token。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null) {
            return null;
        }
        Matcher matcher = BEARER_TOKEN_PATTERN.matcher(authorization);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
