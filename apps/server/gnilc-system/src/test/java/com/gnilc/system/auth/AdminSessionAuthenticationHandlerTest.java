package com.gnilc.system.auth;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.system.session.AdminSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminSessionAuthenticationHandlerTest {
    private final AdminSessionManager sessions = mock(AdminSessionManager.class);
    private final AdminSessionAuthenticationHandler handler = new AdminSessionAuthenticationHandler(sessions);

    @Test
    void bearerTokenSupportIsNamespacedBySessionManager() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(sessions.supportsAccessToken("token")).thenReturn(true);

        assertThat(handler.supports(context(request))).isTrue();

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        assertThat(handler.supports(context(request))).isFalse();
    }

    @Test
    void validBearerTokenCreatesPrincipalAndInvalidTokenFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        when(sessions.validateAccessToken("valid")).thenReturn(12L);

        AuthenticationResult valid = handler.authenticate(context(request));

        assertThat(valid.isAuthenticated()).isTrue();
        assertThat(valid.getPrincipal().getIdentifier()).isEqualTo("12");

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        when(sessions.validateAccessToken("invalid")).thenReturn(null);
        assertThat(handler.authenticate(context(request)).isAuthenticated()).isFalse();
    }

    private ServletAuthenticationContext context(MockHttpServletRequest request) {
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }
}
