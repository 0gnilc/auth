package com.gnilc.authz.web.context;

import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessContextAdapter;
import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import com.gnilc.authz.context.AccessTarget;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 将 Servlet 请求翻译成访问上下文。
 * <p>
 * Servlet 对象不会进入 {@link AccessContext}；这里只抽取授权判断需要的身份和目标事实。
 */
public class ServletAccessContextAdapter implements AccessContextAdapter<HttpServletRequest> {
    private final AccessIdentityResolver<HttpServletRequest> identityResolver;

    /**
     * 创建 Servlet 访问上下文适配器。
     *
     * @param identityResolver Servlet 访问身份解析器
     */
    public ServletAccessContextAdapter(AccessIdentityResolver<HttpServletRequest> identityResolver) {
        this.identityResolver = identityResolver;
    }

    @Override
    public AccessContext adapt(HttpServletRequest request) {
        AccessIdentity identity = identityResolver.resolve(request);
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        String normalizedPath = removeContextPath(requestUri, contextPath);
        AccessTarget target = new AccessTarget(normalizedPath, null, Map.of(
                "rawUri", requestUri,
                "contextPath", contextPath,
                "method", request.getMethod()
        ));
        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put("source", "servlet");
        return new AccessContext(identity, target, attributes);
    }

    /**
     * 将部署上下文从请求 URI 中移除，使 RBAC 匹配只面对应用内路径。
     */
    private String removeContextPath(String requestUri, String contextPath) {
        if (contextPath == null || contextPath.isBlank()) {
            return requestUri;
        }
        if (requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
