package com.gnilc.authz.web.context;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 默认 Servlet 访问身份解析器。
 * <p>
 * 默认实现只表达匿名访问；应用可通过注册自己的 {@link AccessIdentityResolver} 提供登录身份。
 */
public class ServletAccessIdentityResolver implements AccessIdentityResolver<HttpServletRequest> {
    @Override
    public AccessIdentity resolve(HttpServletRequest request) {
        return new AccessIdentity(null, Map.of("anonymous", true));
    }
}
