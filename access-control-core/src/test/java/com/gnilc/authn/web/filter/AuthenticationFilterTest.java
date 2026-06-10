package com.gnilc.authn.web.filter;

import com.gnilc.authn.web.context.ServletAuthenticationContext;
import com.gnilc.authn.web.handler.AuthenticationHandler;
import com.gnilc.authn.web.handler.AuthenticationResult;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    // 没有处理器支持当前请求时，认证过滤器保持可选并继续后续链路。
    @Test
    void continueFilterChainWhenNoHandlerSupportsRequest() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicBoolean failureHandled = new AtomicBoolean(false);
        AuthenticationFilter filter = new AuthenticationFilter(
                List.of(new UnsupportedAuthenticationHandler()),
                (context, result) -> failureHandled.set(true)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isTrue();
        assertThat(failureHandled).isFalse();
    }

    // 认证成功后，过滤器暴露 Principal、继续链路，并停止尝试后续处理器。
    @Test
    void exposePrincipalAndStopTryingHandlersWhenAuthenticationSucceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<ServletRequest> chainRequest = new AtomicReference<>();
        AtomicBoolean laterHandlerCalled = new AtomicBoolean(false);
        AuthenticationFilter filter = new AuthenticationFilter(
                List.of(
                        new UnsupportedAuthenticationHandler(),
                        new SuccessfulAuthenticationHandler("1001"),
                        new TrackingAuthenticationHandler(laterHandlerCalled)
                ),
                (context, result) -> {
                }
        );

        filter.doFilter(request, new MockHttpServletResponse(), (candidate, response) -> chainRequest.set(candidate));

        assertThat(chainRequest.get()).isNotSameAs(request).isInstanceOf(HttpServletRequest.class);
        HttpServletRequest wrappedRequest = (HttpServletRequest) chainRequest.get();
        assertThat(wrappedRequest.getUserPrincipal().getName()).isEqualTo("1001");
        assertThat(wrappedRequest.getRemoteUser()).isEqualTo("1001");
        assertThat(laterHandlerCalled).isFalse();
    }

    // 认证失败时，过滤器交给认证失败处理器并停止链路。
    @Test
    void handleFailureResultAndStopFilterChain() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AuthenticationResult> handledResult = new AtomicReference<>();
        AuthenticationFilter filter = new AuthenticationFilter(
                List.of(new FailingAuthenticationHandler("bad credential")),
                (context, result) -> handledResult.set(result)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledResult.get().isAuthenticated()).isFalse();
        assertThat(handledResult.get().getReason()).isEqualTo("bad credential");
    }

    // 处理器异常视为认证失败，仍不进入后续链路。
    @Test
    void handleExceptionAsAuthenticationFailure() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AuthenticationResult> handledResult = new AtomicReference<>();
        AuthenticationFilter filter = new AuthenticationFilter(
                List.of(new BrokenAuthenticationHandler()),
                (context, result) -> handledResult.set(result)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledResult.get().isAuthenticated()).isFalse();
        assertThat(handledResult.get().getCause()).isInstanceOf(IllegalStateException.class);
    }

    // 处理器按照 Spring order 排序，优先级高的策略先执行。
    @Test
    void authenticateWithSpringOrderedHandlers() throws Exception {
        List<String> events = new ArrayList<>();
        AuthenticationFilter filter = new AuthenticationFilter(
                List.of(
                        new OrderedAuthenticationHandler("late", 20, events),
                        new OrderedAuthenticationHandler("early", 10, events)
                ),
                (context, result) -> {
                }
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        });

        assertThat(events).containsExactly("early");
    }

    private record UnsupportedAuthenticationHandler() implements AuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return false;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new AssertionError("unsupported handler must not authenticate");
        }
    }

    private record SuccessfulAuthenticationHandler(String principalName) implements AuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            Principal principal = () -> principalName;
            return AuthenticationResult.authenticated(principal);
        }
    }

    private record TrackingAuthenticationHandler(AtomicBoolean called) implements AuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            called.set(true);
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            called.set(true);
            return AuthenticationResult.failed("should not be called");
        }
    }

    private record FailingAuthenticationHandler(String reason) implements AuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            return AuthenticationResult.failed(reason);
        }
    }

    private record BrokenAuthenticationHandler() implements AuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new IllegalStateException("broken credential source");
        }
    }

    private record OrderedAuthenticationHandler(String name, int order, List<String> events) implements AuthenticationHandler, Ordered {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            events.add(name);
            return AuthenticationResult.authenticated(() -> name);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
