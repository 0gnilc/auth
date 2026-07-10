package com.gnilc.auth.authn.servlet.filter;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.DefaultServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServletAuthenticationFilterTest {

    @Test
    void successfulAuthenticationWrapsRequestWithPrincipalAndStopsHandlerSearch() throws Exception {
        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of("user-1");
        ServletAuthenticationHandler unsupported = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationHandler successful = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationHandler later = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(unsupported.supports(any())).thenReturn(false);
        when(successful.supports(any())).thenReturn(true);
        when(successful.authenticate(any())).thenReturn(AuthenticationResult.authenticated(principal));

        new ServletAuthenticationFilter(List.of(unsupported, successful, later), failureHandler)
                .doFilter(request, response, chain);

        verify(unsupported).supports(any());
        verify(unsupported, never()).authenticate(any());
        verify(later, never()).supports(any());
        verify(failureHandler, never()).handle(any(), any());
        verify(chain).doFilter(
                org.mockito.ArgumentMatchers.argThat(filteredRequest -> {
                    HttpServletRequest httpRequest = (HttpServletRequest) filteredRequest;
                    return httpRequest.getUserPrincipal() == principal
                            && "user-1".equals(httpRequest.getRemoteUser());
                }),
                same(response)
        );
    }

    @Test
    void failedAuthenticationDelegatesFailureAndStopsChain() throws Exception {
        ServletAuthenticationHandler handler = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        AuthenticationResult failure = AuthenticationResult.failed("expired");
        when(handler.supports(any())).thenReturn(true);
        when(handler.authenticate(any())).thenReturn(failure);

        new ServletAuthenticationFilter(List.of(handler), failureHandler)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(failureHandler).handle(any(ServletAuthenticationContext.class), same(failure));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void nullAuthenticationResultDelegatesFailureAndStopsChain() throws Exception {
        ServletAuthenticationHandler handler = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        when(handler.supports(any())).thenReturn(true);
        when(handler.authenticate(any())).thenReturn(null);

        new ServletAuthenticationFilter(List.of(handler), failureHandler)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(failureHandler).handle(
                any(ServletAuthenticationContext.class),
                org.mockito.ArgumentMatchers.argThat(result ->
                        !result.isAuthenticated() && "authentication failed".equals(result.getReason()))
        );
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void handlerExceptionBecomesFailedAuthenticationResult() throws Exception {
        ServletAuthenticationHandler handler = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        IllegalStateException failure = new IllegalStateException("bad token");
        when(handler.supports(any())).thenReturn(true);
        when(handler.authenticate(any())).thenThrow(failure);

        new ServletAuthenticationFilter(List.of(handler), failureHandler)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(failureHandler).handle(
                any(ServletAuthenticationContext.class),
                org.mockito.ArgumentMatchers.argThat(result ->
                        !result.isAuthenticated()
                                && "bad token".equals(result.getReason())
                                && result.getCause() == failure)
        );
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void requestContinuesUnchangedWhenNoHandlerSupportsIt() throws Exception {
        ServletAuthenticationHandler handler = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(handler.supports(any())).thenReturn(false);

        new ServletAuthenticationFilter(List.of(handler), failureHandler)
                .doFilter(request, response, chain);

        verify(chain).doFilter(same(request), same(response));
        verify(failureHandler, never()).handle(any(), any());
    }

    @Test
    void defaultFailureResponseUsesReasonAndFallbackMessage() throws Exception {
        ServletAuthenticationHandler reasonedFailure = mock(ServletAuthenticationHandler.class);
        when(reasonedFailure.supports(any())).thenReturn(true);
        when(reasonedFailure.authenticate(any())).thenReturn(AuthenticationResult.failed("token expired"));
        MockHttpServletResponse reasonedResponse = new MockHttpServletResponse();

        new ServletAuthenticationFilter(
                List.of(reasonedFailure),
                new DefaultServletAuthenticationFailureHandler()
        ).doFilter(new MockHttpServletRequest(), reasonedResponse, mock(FilterChain.class));

        assertThat(reasonedResponse.getStatus()).isEqualTo(401);
        assertThat(reasonedResponse.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(reasonedResponse.getContentAsString()).isEqualTo("token expired");

        ServletAuthenticationHandler blankFailure = mock(ServletAuthenticationHandler.class);
        when(blankFailure.supports(any())).thenReturn(true);
        when(blankFailure.authenticate(any())).thenReturn(AuthenticationResult.failed("  "));
        MockHttpServletResponse fallbackResponse = new MockHttpServletResponse();

        new ServletAuthenticationFilter(
                List.of(blankFailure),
                new DefaultServletAuthenticationFailureHandler()
        ).doFilter(new MockHttpServletRequest(), fallbackResponse, mock(FilterChain.class));

        assertThat(fallbackResponse.getContentAsString()).isEqualTo("authentication failed");
    }

    @Test
    void handlersRunInSpringOrder() throws Exception {
        List<String> calls = new ArrayList<>();
        ServletAuthenticationFailureHandler failureHandler = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);

        new ServletAuthenticationFilter(
                List.of(new LastHandler(calls), new FirstHandler(calls)),
                failureHandler
        ).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(calls).containsExactly("first", "last");
        verify(chain).doFilter(any(), any());
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    private static final class FirstHandler implements ServletAuthenticationHandler {
        private final List<String> calls;

        private FirstHandler(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public boolean supports(ServletAuthenticationContext context) {
            calls.add("first");
            return false;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new AssertionError("unsupported handler must not authenticate");
        }
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    private static final class LastHandler implements ServletAuthenticationHandler {
        private final List<String> calls;

        private LastHandler(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public boolean supports(ServletAuthenticationContext context) {
            calls.add("last");
            return false;
        }

        @Override
        public AuthenticationResult authenticate(ServletAuthenticationContext context) {
            throw new AssertionError("unsupported handler must not authenticate");
        }
    }
}
