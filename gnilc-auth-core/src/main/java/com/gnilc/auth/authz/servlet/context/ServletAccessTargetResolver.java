package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessTargetResolver;

/**
 * Servlet 访问目标解析 seam。
 * <p>
 * 该类型用于在 Spring Bean 边界上隔离 Servlet 目标解析器，避免其他执行环境的
 * {@link AccessTargetResolver} 影响 Servlet 授权自动配置。
 */
@FunctionalInterface
public interface ServletAccessTargetResolver extends AccessTargetResolver<ServletRequestContext> {
}
