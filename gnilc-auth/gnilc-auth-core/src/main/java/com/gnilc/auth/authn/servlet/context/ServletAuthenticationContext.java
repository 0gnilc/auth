package com.gnilc.auth.authn.servlet.context;

import com.google.common.base.Preconditions;
import com.gnilc.auth.authn.context.AuthenticationContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 认证上下文。
 * <p>
 * 认证处理器只读取请求凭证并写出认证失败响应，不参与授权判断。
 */
public class ServletAuthenticationContext implements AuthenticationContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * 创建 Servlet 认证上下文。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    public ServletAuthenticationContext(HttpServletRequest request, HttpServletResponse response) {
        Preconditions.checkArgument(request != null, "request == null!");
        Preconditions.checkArgument(response != null, "response == null!");
        this.request = request;
        this.response = response;
    }

    /**
     * @return HTTP 请求
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return HTTP 响应
     */
    public HttpServletResponse getResponse() {
        return response;
    }
}
