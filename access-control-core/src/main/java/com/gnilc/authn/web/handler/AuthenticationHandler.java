package com.gnilc.authn.web.handler;

import com.gnilc.authn.web.context.ServletAuthenticationContext;

/**
 * Servlet 认证处理器。
 * <p>
 * 一个处理器负责一种认证来源；认证通过后由过滤器将主体暴露给后续链路。
 */
public interface AuthenticationHandler {
    /**
     * 判断当前处理器是否支持该请求。
     *
     * @param context Servlet 认证上下文
     * @return 是否支持当前请求
     */
    boolean supports(ServletAuthenticationContext context);

    /**
     * 执行认证。
     *
     * @param context Servlet 认证上下文
     * @return 认证结果
     */
    AuthenticationResult authenticate(ServletAuthenticationContext context) throws Exception;
}
