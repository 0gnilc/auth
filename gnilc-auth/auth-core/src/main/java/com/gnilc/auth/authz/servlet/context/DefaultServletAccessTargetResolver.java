package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessTarget;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 默认 Servlet 访问目标解析器。
 * <p>
 * 该 resolver 是 {@link ServletAccessContextAdapter} 内部的目标提取辅助，只负责从 Servlet 请求解析访问目标。
 */
public class DefaultServletAccessTargetResolver implements ServletAccessTargetResolver {
    @Override
    public AccessTarget resolve(ServletRequestContext context) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        String targetIdentifier = requestUri;
        if (!contextPath.isEmpty() && requestUri != null && requestUri.startsWith(contextPath)) {
            targetIdentifier = requestUri.substring(contextPath.length());
            if (targetIdentifier.isEmpty()) {
                targetIdentifier = "/";
            }
        }
        return new AccessTarget(targetIdentifier, request.getMethod());
    }
}
