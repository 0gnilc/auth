package com.gnilc.auth.authz.servlet.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Servlet 请求参数包装对象。
 * <p>
 * 该对象仅用于包装一次 Servlet 请求处理过程中的 request、response 和 chain，
 * 不定义认证、授权或流程控制语义。
 */
public class ServletRequestContext {
    private final ServletRequest request;
    private final ServletResponse response;
    private final FilterChain chain;

    /**
     * 创建 Servlet 请求参数包装对象。
     *
     * @param request  Servlet 请求
     * @param response Servlet 响应
     * @param chain    Servlet 过滤器链
     */
    public ServletRequestContext(ServletRequest request, ServletResponse response, FilterChain chain) {
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
