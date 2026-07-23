package com.gnilc.auth.authn.servlet.filter;

import com.google.common.base.Preconditions;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet 认证过滤器。
 * <p>
 * 过滤器只负责编排认证处理器；认证成功后继续后续链路，认证失败时停止链路。
 */
public class ServletAuthenticationFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ServletAuthenticationFilter.class);

    private final List<ServletAuthenticationHandler> handlers;
    private final ServletAuthenticationFailureHandler failureHandler;

    /**
     * 创建 Servlet 认证过滤器。
     *
     * @param handlers       认证处理器列表
     * @param failureHandler 认证失败处理器
     */
    public ServletAuthenticationFilter(List<ServletAuthenticationHandler> handlers, ServletAuthenticationFailureHandler failureHandler) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(handlers), "handlers is empty!");
        Preconditions.checkArgument(failureHandler != null, "failureHandler == null!");
        List<ServletAuthenticationHandler> orderedHandlers = new ArrayList<>(handlers);
        AnnotationAwareOrderComparator.sort(orderedHandlers);
        this.handlers = List.copyOf(orderedHandlers);
        this.failureHandler = failureHandler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ServletAuthenticationContext context = new ServletAuthenticationContext(httpRequest, httpResponse);
        for (ServletAuthenticationHandler handler : handlers) {
            AuthenticationResult result;
            try {
                if (!handler.supports(context)) {
                    continue;
                }
                result = handler.authenticate(context);
            } catch (Exception e) {
                log.error("Authentication processing failed", e);
                failureHandler.handle(context, AuthenticationResult.failed(null, e));
                return;
            }
            if (result != null && result.isAuthenticated()) {
                chain.doFilter(new AuthenticatedHttpServletRequest(httpRequest, result.getPrincipal()), response);
                return;
            }
            failureHandler.handle(context, result == null ? AuthenticationResult.failed(null) : result);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 携带认证主体的 Servlet 请求包装器。
     */
    private static class AuthenticatedHttpServletRequest extends HttpServletRequestWrapper {
        private final Principal principal;

        private AuthenticatedHttpServletRequest(HttpServletRequest request, Principal principal) {
            super(request);
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public String getRemoteUser() {
            return principal == null ? null : principal.getName();
        }
    }
}
