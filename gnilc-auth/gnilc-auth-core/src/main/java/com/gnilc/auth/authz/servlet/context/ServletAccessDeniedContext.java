package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.denied.AccessDeniedContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Servlet 访问拒绝上下文。
 * <p>
 * 该对象只交给 Web 拒绝处理器使用，不进入授权决策流程。
 */
public class ServletAccessDeniedContext extends ServletRequestContext implements AccessDeniedContext {

    /**
     * 创建 Servlet 访问拒绝上下文。
     *
     * @param request  Servlet 请求
     * @param response Servlet 响应
     * @param chain    Servlet 过滤器链
     */
    public ServletAccessDeniedContext(ServletRequest request, ServletResponse response, FilterChain chain) {
        super(request, response, chain);
    }

    /**
     * 创建 Servlet 访问拒绝上下文。
     *
     * @param context Servlet 请求参数包装对象
     */
    public ServletAccessDeniedContext(ServletRequestContext context) {
        super(context.getRequest(), context.getResponse(), context.getChain());
    }
}
