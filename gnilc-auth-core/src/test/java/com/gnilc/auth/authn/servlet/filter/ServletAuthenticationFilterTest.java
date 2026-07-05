package com.gnilc.auth.authn.servlet.filter;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ServletAuthenticationFilterTest {

    // 没有处理器支持当前请求时，认证过滤器保持可选并继续后续链路。
    // TestCaseId: CORE-AUTHN-015
    @Test
    void continueFilterChainWhenNoHandlerSupportsRequest() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicBoolean failureHandled = new AtomicBoolean(false);
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(new UnsupportedServletAuthenticationHandler()),
                (context, result) -> failureHandled.set(true)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isTrue();
        assertThat(failureHandled).isFalse();
    }

    // 认证成功后，过滤器暴露 Principal、继续链路，并停止尝试后续处理器。
    // TestCaseId: CORE-AUTHN-016
    @Test
    void exposePrincipalAndStopTryingHandlersWhenAuthenticationSucceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<ServletRequest> chainRequest = new AtomicReference<>();
        AtomicBoolean laterHandlerCalled = new AtomicBoolean(false);
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(
                        new UnsupportedServletAuthenticationHandler(),
                        new SuccessfulServletAuthenticationHandler("1001"),
                        new TrackingServletAuthenticationHandler(laterHandlerCalled)
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
    // TestCaseId: CORE-AUTHN-017
    @Test
    void handleFailureResultAndStopFilterChain() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AuthenticationResult> handledResult = new AtomicReference<>();
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(new FailingServletAuthenticationHandler("bad credential")),
                (context, result) -> handledResult.set(result)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledResult.get().isAuthenticated()).isFalse();
        assertThat(handledResult.get().getReason()).isEqualTo("bad credential");
    }

    // 处理器异常视为认证失败，仍不进入后续链路。
    // TestCaseId: CORE-AUTHN-018
    @Test
    void handleExceptionAsAuthenticationFailure() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AuthenticationResult> handledResult = new AtomicReference<>();
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(new BrokenServletAuthenticationHandler()),
                (context, result) -> handledResult.set(result)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledResult.get().isAuthenticated()).isFalse();
        assertThat(handledResult.get().getCause()).isInstanceOf(IllegalStateException.class);
    }

    // 处理器按照 Spring order 排序，优先级高的策略先执行。
    // TestCaseId: CORE-AUTHN-019
    @Test
    void authenticateWithSpringOrderedHandlers() throws Exception {
        List<String> events = new ArrayList<>();
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(
                        new OrderedServletAuthenticationHandler("late", 20, events),
                        new OrderedServletAuthenticationHandler("early", 10, events)
                ),
                (context, result) -> {
                }
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        });

        assertThat(events).containsExactly("early");
    }

    // 支持当前请求的处理器返回 null 时，应按认证失败处理并停止链路。
    // TestCaseId: CORE-AUTHN-020
    @Test
    void handleNullAuthenticationResultAsFailure() throws Exception {
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AuthenticationResult> handledResult = new AtomicReference<>();
        ServletAuthenticationFilter filter = new ServletAuthenticationFilter(
                List.of(new NullResultServletAuthenticationHandler()),
                (context, result) -> handledResult.set(result)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledResult.get().isAuthenticated()).isFalse();
        assertThat(handledResult.get().getReason()).isEqualTo("authentication failed");
    }

    // 认证过滤器必须有至少一个处理器和失败处理器，避免启动时形成无效链路。
    // TestCaseId: CORE-AUTHN-021
    @Test
    void requireHandlersAndFailureHandler() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ServletAuthenticationFilter(List.of(), (context, result) -> {
                }))
                .withMessage("handlers is empty!");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ServletAuthenticationFilter(List.of(new UnsupportedServletAuthenticationHandler()), null))
                .withMessage("failureHandler == null!");
    }

    private record UnsupportedServletAuthenticationHandler() implements ServletAuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return false;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new AssertionError("unsupported handler must not authenticate");
        }
    }

    private record SuccessfulServletAuthenticationHandler(String principalName) implements ServletAuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            return AuthenticationResult.authenticated(DefaultAccessPrincipal.of(principalName));
        }
    }

    private record TrackingServletAuthenticationHandler(AtomicBoolean called) implements ServletAuthenticationHandler {
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

    private record FailingServletAuthenticationHandler(String reason) implements ServletAuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            return AuthenticationResult.failed(reason);
        }
    }

    private record NullResultServletAuthenticationHandler() implements ServletAuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            return null;
        }
    }

    private record BrokenServletAuthenticationHandler() implements ServletAuthenticationHandler {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new IllegalStateException("broken credential source");
        }
    }

    private record OrderedServletAuthenticationHandler(String name, int order, List<String> events) implements ServletAuthenticationHandler, Ordered {
        @Override
        public boolean supports(ServletAuthenticationContext context) {
            return true;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            events.add(name);
            return AuthenticationResult.authenticated(DefaultAccessPrincipal.of(name));
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
