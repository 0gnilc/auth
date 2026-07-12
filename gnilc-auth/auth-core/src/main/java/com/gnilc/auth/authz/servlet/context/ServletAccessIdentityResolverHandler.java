package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessIdentityResolverHandler;

/**
 * Servlet 访问身份解析处理器 seam。
 * <p>
 * 该类型用于在 Spring Bean 边界上隔离 Servlet handler，避免其他执行环境的 handler 进入
 * Servlet 身份解析链路。
 */
public interface ServletAccessIdentityResolverHandler extends AccessIdentityResolverHandler<ServletRequestContext> {
}
