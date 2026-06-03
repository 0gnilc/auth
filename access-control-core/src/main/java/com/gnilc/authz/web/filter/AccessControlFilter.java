package com.gnilc.authz.web.filter;

import com.google.common.base.Preconditions;
import com.gnilc.authz.decision.AccessDecision;
import com.gnilc.authz.denied.AccessDenied;
import jakarta.servlet.*;


import java.io.IOException;

/**
 * 权限控制过滤器
 */
public class AccessControlFilter implements Filter {
    public static final int REGISTRATION_ORDER = Integer.MAX_VALUE;
    public static final int REGISTRATION_ORDER_PREVIOUS = REGISTRATION_ORDER - 1;
    /**
     * 访问决策器
     */
    private final AccessDecision accessDecision;
    /**
     * 访问拒绝器
     */
    private final AccessDenied accessDenied;

    public AccessControlFilter(final AccessDecision accessDecision, final AccessDenied accessDenied) {
        Preconditions.checkArgument(accessDecision != null, "accessDecision == null!");
        Preconditions.checkArgument(accessDenied != null, "accessDenied == null!");
        this.accessDecision = accessDecision;
        this.accessDenied = accessDenied;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (accessDecision.decide()) {
            chain.doFilter(request, response);
        } else {
            accessDenied.denied(new FilterWrapper(request, response, chain));
        }
    }
}
