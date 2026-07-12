package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 默认 Servlet 访问身份解析处理器。
 * <p>
 * 该 handler 从 Servlet 请求中的认证 Principal 读取身份事实，作为 Servlet 环境下已认证请求的默认兜底解析规则。
 */
public class DefaultServletAccessIdentityResolverHandler implements ServletAccessIdentityResolverHandler {
    /**
     * 判断请求是否携带认证 Principal。
     */
    @Override
    public boolean supports(ServletRequestContext context) {
        return context.getRequest() instanceof HttpServletRequest request
                && request.getUserPrincipal() instanceof AccessPrincipal principal
                && StringUtils.hasText(principal.getIdentifier());
    }

    /**
     * 转换为访问身份。
     */
    @Override
    public AccessIdentity handle(ServletRequestContext context) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        AccessPrincipal principal = (AccessPrincipal) request.getUserPrincipal();
        Map<String, Object> attributes = Maps.newHashMap(principal.getAttributes());
        attributes.put("principal", true);
        return new AccessIdentity(principal.getIdentifier(), attributes);
    }
}
