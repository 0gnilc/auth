package com.gnilc.authn.web.filter;

import com.google.common.base.Preconditions;
import com.gnilc.authn.web.context.ServletAuthenticationContext;
import com.gnilc.authn.web.handler.AuthenticationFailureHandler;
import com.gnilc.authn.web.handler.AuthenticationHandler;
import com.gnilc.authn.web.handler.AuthenticationResult;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
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
public class AuthenticationFilter implements Filter {
    private final List<AuthenticationHandler> handlers;
    private final AuthenticationFailureHandler failureHandler;

    /**
     * 创建 Servlet 认证过滤器。
     *
     * @param handlers       认证处理器列表
     * @param failureHandler 认证失败处理器
     */
    public AuthenticationFilter(List<AuthenticationHandler> handlers, AuthenticationFailureHandler failureHandler) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(handlers), "handlers is empty!");
        Preconditions.checkArgument(failureHandler != null, "failureHandler == null!");
        List<AuthenticationHandler> orderedHandlers = new ArrayList<>(handlers);
        AnnotationAwareOrderComparator.sort(orderedHandlers);
        this.handlers = List.copyOf(orderedHandlers);
        this.failureHandler = failureHandler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ServletAuthenticationContext context = new ServletAuthenticationContext(httpRequest, httpResponse);
        for (AuthenticationHandler handler : handlers) {
            AuthenticationResult result;
            try {
                if (!handler.supports(context)) {
                    continue;
                }
                result = handler.authenticate(context);
            } catch (Exception e) {
                failureHandler.handle(context, AuthenticationResult.failed(e.getMessage(), e));
                return;
            }
            if (result != null && result.isAuthenticated()) {
                chain.doFilter(new AuthenticatedHttpServletRequest(httpRequest, result.getPrincipal()), response);
                return;
            }
            failureHandler.handle(context, result == null ? AuthenticationResult.failed("authentication failed") : result);
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
