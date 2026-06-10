package com.gnilc.authz.web.filter;

import com.google.common.base.Preconditions;
import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessContextAdapter;
import com.gnilc.authz.decision.AccessDecision;
import com.gnilc.authz.denied.AccessDeniedHandler;
import com.gnilc.authz.web.context.FilterDeniedContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;


import java.io.IOException;

/**
 * 授权过滤器。
 * <p>
 * 过滤器只负责连接 Servlet 环境和授权核心：构造访问上下文、执行授权决策、分派拒绝处理。
 */
public class AuthorizationFilter implements Filter {
    public static final int REGISTRATION_ORDER = Integer.MAX_VALUE;
    public static final int REGISTRATION_ORDER_PREVIOUS = REGISTRATION_ORDER - 1;
    /**
     * 访问上下文适配器。
     */
    private final AccessContextAdapter<HttpServletRequest> accessContextAdapter;
    /**
     * 访问决策器。
     */
    private final AccessDecision accessDecision;
    /**
     * 访问拒绝处理器。
     */
    private final AccessDeniedHandler<FilterDeniedContext> accessDeniedHandler;

    /**
     * 创建授权过滤器。
     *
     * @param accessContextAdapter 访问上下文适配器
     * @param accessDecision       访问决策器
     * @param accessDeniedHandler  访问拒绝处理器
     */
    public AuthorizationFilter(final AccessContextAdapter<HttpServletRequest> accessContextAdapter,
                               final AccessDecision accessDecision,
                               final AccessDeniedHandler<FilterDeniedContext> accessDeniedHandler) {
        Preconditions.checkArgument(accessContextAdapter != null, "accessContextAdapter == null!");
        Preconditions.checkArgument(accessDecision != null, "accessDecision == null!");
        Preconditions.checkArgument(accessDeniedHandler != null, "accessDeniedHandler == null!");
        this.accessContextAdapter = accessContextAdapter;
        this.accessDecision = accessDecision;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        AccessContext context = accessContextAdapter.adapt((HttpServletRequest) request);
        if (accessDecision.decide(context)) {
            chain.doFilter(request, response);
            return;
        }
        // 拒绝处理保留 Servlet 上下文，但授权核心只接收 AccessContext。
        accessDeniedHandler.handle(context, new FilterDeniedContext(request, response, chain));
    }
}
