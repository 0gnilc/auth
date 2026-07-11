package com.gnilc.auth.authz;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.denied.AccessDenied;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServletAuthorizationFilterTest {
    private final AccessContext accessContext =
            new AccessContext(new AccessIdentity("3", null), new AccessTarget("/secure", "GET"));

    @Test
    void allowedRequestContinuesWithoutDeniedHandling() throws Exception {
        AccessDecision decision = mock(AccessDecision.class);
        ServletAccessContextAdapter adapter = ignored -> accessContext;
        AccessDenied denied = mock(AccessDenied.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(decision.decide(accessContext)).thenReturn(true);

        new ServletAuthorizationFilter(decision, adapter, denied).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(denied, never()).denied(any(), any());
    }

    @Test
    void deniedRequestDelegatesServletDeniedContextAndStopsChain() throws Exception {
        AccessDecision decision = ignored -> false;
        ServletAccessContextAdapter adapter = ignored -> accessContext;
        AccessDenied denied = mock(AccessDenied.class);
        FilterChain chain = mock(FilterChain.class);

        new ServletAuthorizationFilter(decision, adapter, denied)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(denied).denied(org.mockito.ArgumentMatchers.same(accessContext),
                any(ServletAccessDeniedContext.class));
        verify(chain, never()).doFilter(any(), any());
    }
}
