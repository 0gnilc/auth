package com.gnilc.auth.authn;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.DefaultAccessPrincipalHolder;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.DefaultServletAuthenticationFailureHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationModelTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void principalAndSuccessfulResultExposeImmutableSnapshots() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("tenant", "north");
        AccessPrincipal principal = DefaultAccessPrincipal.of(42L, facts);
        AuthenticationResult result = AuthenticationResult.authenticated(principal, facts);

        facts.put("tenant", "south");

        assertThat(principal.getIdentifier()).isEqualTo("42");
        assertThat(principal.getName()).isEqualTo("42");
        assertThat(principal.getAttributes()).containsEntry("tenant", "north");
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(principal);
        assertThat(result.getAttributes()).containsEntry("tenant", "north");
        assertThatThrownBy(() -> result.getAttributes().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failedResultCarriesReasonAndCauseWithoutPrincipal() {
        IllegalStateException cause = new IllegalStateException("offline");

        AuthenticationResult result = AuthenticationResult.failed("invalid token", cause);

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getPrincipal()).isNull();
        assertThat(result.getReason()).isEqualTo("invalid token");
        assertThat(result.getCause()).isSameAs(cause);
        assertThat(result.getAttributes()).isEmpty();
    }

    @Test
    void servletContextRejectsMissingRequestOrResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> new ServletAuthenticationContext(null, response))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServletAuthenticationContext(request, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultFailureHandlerWritesUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAuthenticationContext context =
                new ServletAuthenticationContext(new MockHttpServletRequest(), response);

        new DefaultServletAuthenticationFailureHandler().handle(context, AuthenticationResult.failed("expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("expired");
    }

    @Test
    void principalHolderReadsOnlyAccessPrincipalFromCurrentServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessPrincipal principal = DefaultAccessPrincipal.of("admin");
        request.setUserPrincipal(principal);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(DefaultAccessPrincipalHolder.getPrincipal()).isSameAs(principal);

        RequestContextHolder.resetRequestAttributes();
        assertThat(DefaultAccessPrincipalHolder.getPrincipal()).isNull();
    }
}
