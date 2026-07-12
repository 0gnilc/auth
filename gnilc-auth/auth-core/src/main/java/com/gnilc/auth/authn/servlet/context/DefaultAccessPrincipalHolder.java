package com.gnilc.auth.authn.servlet.context;

import com.gnilc.auth.authn.context.AccessPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前 Servlet 请求访问 Principal 静态读取工具。
 */
public final class DefaultAccessPrincipalHolder {
    private DefaultAccessPrincipalHolder() {
    }

    /**
     * 读取当前请求中的访问 Principal。
     *
     * @return 当前请求访问 Principal，未认证时可能为 {@code null}
     */
    public static AccessPrincipal getPrincipal() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        if (!(request.getUserPrincipal() instanceof AccessPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
