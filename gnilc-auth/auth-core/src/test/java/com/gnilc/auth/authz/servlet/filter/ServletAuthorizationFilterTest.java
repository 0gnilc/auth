package com.gnilc.auth.authz.servlet.filter;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import com.gnilc.auth.authz.servlet.context.ServletRequestContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServletAuthorizationFilterTest {

    @Test
    void allowedRequestContinuesFilterChain() throws Exception {
        AccessDecision accessDecision = mock(AccessDecision.class);
        ServletAccessContextAdapter adapter = mock(ServletAccessContextAdapter.class);
        AccessDenied accessDenied = mock(AccessDenied.class);
        FilterChain chain = mock(FilterChain.class);
        AccessContext accessContext = mock(AccessContext.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(adapter.adapt(any())).thenReturn(accessContext);
        when(accessDecision.decide(accessContext)).thenReturn(true);

        new ServletAuthorizationFilter(accessDecision, adapter, accessDenied)
                .doFilter(request, response, chain);

        verify(adapter).adapt(org.mockito.ArgumentMatchers.argThat(context ->
                context.getRequest() == request
                        && context.getResponse() == response
                        && context.getChain() == chain));
        verify(accessDecision).decide(accessContext);
        verify(chain).doFilter(same(request), same(response));
        verify(accessDenied, never()).denied(any(), any());
    }

    @Test
    void deniedRequestDelegatesServletDeniedContextAndStopsChain() throws Exception {
        AccessDecision accessDecision = mock(AccessDecision.class);
        ServletAccessContextAdapter adapter = mock(ServletAccessContextAdapter.class);
        AccessDenied accessDenied = mock(AccessDenied.class);
        FilterChain chain = mock(FilterChain.class);
        AccessContext accessContext = mock(AccessContext.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(adapter.adapt(any(ServletRequestContext.class))).thenReturn(accessContext);
        when(accessDecision.decide(accessContext)).thenReturn(false);

        new ServletAuthorizationFilter(accessDecision, adapter, accessDenied)
                .doFilter(request, response, chain);

        verify(accessDenied).denied(
                same(accessContext),
                org.mockito.ArgumentMatchers.argThat((ServletAccessDeniedContext context) ->
                        context.getRequest() == request
                                && context.getResponse() == response
                                && context.getChain() == chain)
        );
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void decisionExceptionPropagatesWithoutContinuingOrDenying() throws Exception {
        AccessDecision accessDecision = mock(AccessDecision.class);
        ServletAccessContextAdapter adapter = mock(ServletAccessContextAdapter.class);
        AccessDenied accessDenied = mock(AccessDenied.class);
        FilterChain chain = mock(FilterChain.class);
        AccessContext accessContext = mock(AccessContext.class);
        IllegalStateException failure = new IllegalStateException("permission lookup failed");
        when(adapter.adapt(any(ServletRequestContext.class))).thenReturn(accessContext);
        when(accessDecision.decide(accessContext)).thenThrow(failure);

        ServletAuthorizationFilter filter = new ServletAuthorizationFilter(accessDecision, adapter, accessDenied);

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isSameAs(failure);
        verify(chain, never()).doFilter(any(), any());
        verify(accessDenied, never()).denied(any(), any());
    }
}
