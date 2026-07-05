package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.DelegatingAccessIdentityResolver;

import java.util.List;
import java.util.Map;

/**
 * 默认 Servlet 访问身份解析器。
 * <p>
 * 该 resolver 复用通用 handler 组合规范，按顺序选择第一个支持当前 Servlet 请求的
 * {@link ServletAccessIdentityResolverHandler}；默认配置会将 {@link DefaultServletAccessIdentityResolverHandler}
 * 追加为最后一个 handler，用于把认证 Principal 转换为访问身份。没有 handler 命中时返回匿名身份。
 */
public class DefaultServletAccessIdentityResolver extends DelegatingAccessIdentityResolver<ServletRequestContext>
        implements ServletAccessIdentityResolver {

    /**
     * 创建默认 Servlet 访问身份解析器。
     *
     * @param handlers Servlet 访问身份解析处理器列表
     */
    public DefaultServletAccessIdentityResolver(List<ServletAccessIdentityResolverHandler> handlers) {
        super(handlers, context -> new AccessIdentity(null, Map.of("anonymous", true)));
    }
}
