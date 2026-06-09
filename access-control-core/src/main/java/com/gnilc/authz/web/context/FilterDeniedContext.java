package com.gnilc.authz.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Servlet 过滤器拒绝处理上下文。
 * <p>
 * 该对象只交给 Web 拒绝处理器使用，不进入授权决策流程。
 */
public class FilterDeniedContext {
    private final ServletRequest request;
    private final ServletResponse response;
    private final FilterChain chain;

    /**
     * 创建 Servlet 拒绝处理上下文。
     *
     * @param request  Servlet 请求
     * @param response Servlet 响应
     * @param chain    Servlet 过滤器链
     */
    public FilterDeniedContext(ServletRequest request, ServletResponse response, FilterChain chain) {
        this.request = request;
        this.response = response;
        this.chain = chain;
    }

    /**
     * @return Servlet 请求
     */
    public ServletRequest getRequest() {
        return request;
    }

    /**
     * @return Servlet 响应
     */
    public ServletResponse getResponse() {
        return response;
    }

    /**
     * @return Servlet 过滤器链
     */
    public FilterChain getChain() {
        return chain;
    }
}
