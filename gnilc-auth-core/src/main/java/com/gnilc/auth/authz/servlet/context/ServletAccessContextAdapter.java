package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessContextAdapter;

/**
 * Servlet 访问上下文适配 seam。
 * <p>
 * 该类型用于在 Spring Bean 边界上隔离 Servlet adapter，避免其他执行环境的
 * {@link AccessContextAdapter} 影响 Servlet 授权自动配置。
 */
@FunctionalInterface
public interface ServletAccessContextAdapter extends AccessContextAdapter<ServletRequestContext> {
}
