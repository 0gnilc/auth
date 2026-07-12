package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessIdentityResolver;

/**
 * Servlet 访问身份解析 seam。
 * <p>
 * 该类型用于在 Spring Bean 边界上隔离 Servlet 身份解析器，避免其他执行环境的
 * {@link AccessIdentityResolver} 影响 Servlet 授权自动配置。
 */
@FunctionalInterface
public interface ServletAccessIdentityResolver extends AccessIdentityResolver<ServletRequestContext> {
}
