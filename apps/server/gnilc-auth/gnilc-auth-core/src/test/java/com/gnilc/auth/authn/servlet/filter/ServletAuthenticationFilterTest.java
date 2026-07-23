package com.gnilc.auth.authn.servlet.filter;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServletAuthenticationFilterTest {
    @Test
    void successfulHandlerAddsPrincipalAndStopsHandlerChain() throws Exception {
        ServletAuthenticationHandler unsupported = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationHandler supported = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failures = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Principal> observedPrincipal = new AtomicReference<>();

        when(unsupported.supports(any())).thenReturn(false);
        when(supported.supports(any())).thenReturn(true);
        when(supported.authenticate(any()))
                .thenReturn(AuthenticationResult.authenticated(DefaultAccessPrincipal.of(7L)));
        org.mockito.Mockito.doAnswer(invocation -> {
            observedPrincipal.set(invocation.<jakarta.servlet.http.HttpServletRequest>getArgument(0).getUserPrincipal());
            return null;
        }).when(chain).doFilter(any(), any());

        new ServletAuthenticationFilter(List.of(unsupported, supported), failures)
                .doFilter(request, response, chain);

        assertThat(observedPrincipal.get().getName()).isEqualTo("7");
        verify(failures, never()).handle(any(), any());
    }

    @Test
    void unsupportedCredentialsContinueAsAnonymous() throws Exception {
        ServletAuthenticationHandler handler = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failures = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(handler.supports(any())).thenReturn(false);

        new ServletAuthenticationFilter(List.of(handler), failures).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(failures, never()).handle(any(), any());
    }

    @Test
    void failedOrExceptionalAuthenticationStopsTheChain() throws Exception {
        ServletAuthenticationHandler failed = mock(ServletAuthenticationHandler.class);
        ServletAuthenticationFailureHandler failures = mock(ServletAuthenticationFailureHandler.class);
        FilterChain chain = mock(FilterChain.class);
        when(failed.supports(any())).thenReturn(true);
        when(failed.authenticate(any())).thenThrow(new IllegalStateException("backend unavailable"));

        new ServletAuthenticationFilter(List.of(failed), failures)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(failures).handle(any(ServletAuthenticationContext.class),
                org.mockito.ArgumentMatchers.argThat(result ->
                        !result.isAuthenticated()
                                && result.getReason() == null
                                && result.getCause() instanceof IllegalStateException));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void constructorRequiresHandlersAndFailureHandler() {
        ServletAuthenticationFailureHandler failures = mock(ServletAuthenticationFailureHandler.class);

        assertThatThrownBy(() -> new ServletAuthenticationFilter(List.of(), failures))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServletAuthenticationFilter(
                List.of(mock(ServletAuthenticationHandler.class)), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
