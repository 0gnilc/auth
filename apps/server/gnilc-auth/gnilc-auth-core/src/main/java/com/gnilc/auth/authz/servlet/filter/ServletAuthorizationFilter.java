package com.gnilc.auth.authz.servlet.filter;

import com.google.common.base.Preconditions;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import com.gnilc.auth.authz.servlet.context.ServletRequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * Servlet 授权过滤器。
 * <p>
 * 过滤器只负责连接 Servlet 环境和授权核心：构造访问上下文、执行授权决策、执行访问拒绝。
 */
public class ServletAuthorizationFilter implements Filter {
    /**
     * 访问决策器。
     */
    private final AccessDecision accessDecision;
    /**
     * 访问拒绝入口。
     */
    private final AccessDenied accessDenied;
    /**
     * 访问上下文适配器。
     */
    private final ServletAccessContextAdapter accessContextAdapter;

    /**
     * 创建 Servlet 授权过滤器。
     *
     * @param accessContextAdapter 访问上下文适配器
     * @param accessDecision       访问决策器
     * @param accessDenied         访问拒绝入口
     */
    public ServletAuthorizationFilter(final AccessDecision accessDecision,
                                      final ServletAccessContextAdapter accessContextAdapter,
                                      final AccessDenied accessDenied) {
        Preconditions.checkArgument(accessDecision != null, "accessDecision == null!");
        Preconditions.checkArgument(accessContextAdapter != null, "accessContextAdapter == null!");
        Preconditions.checkArgument(accessDenied != null, "accessDenied == null!");
        this.accessContextAdapter = accessContextAdapter;
        this.accessDecision = accessDecision;
        this.accessDenied = accessDenied;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequestContext servletRequestContext = new ServletRequestContext(request, response, chain);
        AccessContext accessContext = accessContextAdapter.adapt(servletRequestContext);
        if (accessDecision.decide(accessContext)) {
            chain.doFilter(request, response);
            return;
        }
        accessDenied.denied(accessContext, new ServletAccessDeniedContext(servletRequestContext));
    }
}
