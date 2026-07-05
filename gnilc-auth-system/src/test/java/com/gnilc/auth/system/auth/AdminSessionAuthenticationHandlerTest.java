package com.gnilc.auth.system.auth;

import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.system.session.AdminSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionAuthenticationHandlerTest {
    private AdminSessionManager sessionManager;
    private AdminSessionAuthenticationHandler handler;

    @BeforeEach
    void setUp() {
        sessionManager = mock(AdminSessionManager.class);
        handler = new AdminSessionAuthenticationHandler(sessionManager);
    }

    // TestCaseId: SYS-AUTH-001
    @Test
    void supportsOnlyAdminNamespacedBearerToken() {
        when(sessionManager.supportsAccessToken("sys_admin.1001.access-token")).thenReturn(true);
        when(sessionManager.supportsAccessToken("1001.access-token")).thenReturn(false);

        assertThat(handler.supports(context("/api/me", "Bearer sys_admin.1001.access-token"))).isTrue();
        assertThat(handler.supports(context("/unknown", "bearer sys_admin.1001.access-token"))).isTrue();
        assertThat(handler.supports(context("/sys/admin/user-info", "Bearer 1001.access-token"))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", "Bearer"))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", "Bearer "))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", null))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", "Basic sys_admin.1001.access-token"))).isFalse();
    }

    // TestCaseId: SYS-AUTH-002
    @Test
    void authenticatesWithExtractedAccessTokenAndPrincipal() {
        when(sessionManager.validateAccessToken("sys_admin.1001.access-token")).thenReturn(1001L);

        AuthenticationResult result = handler.authenticate(context("/sys/admin/user-info", "Bearer sys_admin.1001.access-token"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(DefaultAccessPrincipal.class);
        assertThat(result.getPrincipal().getName()).isEqualTo("1001");
        assertThat(result.getAttributes()).isEmpty();
        assertThat(result.getPrincipal().getAttributes()).isEmpty();
        verify(sessionManager).validateAccessToken("sys_admin.1001.access-token");
    }

    // TestCaseId: SYS-AUTH-003
    @Test
    void failsWhenAccessTokenIsNotBackedBySession() {
        when(sessionManager.validateAccessToken("sys_admin.1001.missing-token")).thenReturn(null);

        AuthenticationResult result = handler.authenticate(context("/sys/admin/user-info", "Bearer sys_admin.1001.missing-token"));

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getReason()).isEqualTo("invalid access token");
        verify(sessionManager).validateAccessToken("sys_admin.1001.missing-token");
    }

    // TestCaseId: SYS-AUTH-004
    @Test
    void rejectsBearerHeaderWithoutSingleTokenValue() {
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer")).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer ")).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer    ")).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer sys_admin.1001.access-token extra")).isAuthenticated()).isFalse();
    }

    // TestCaseId: SYS-AUTH-005
    @Test
    void authenticateFailsWithoutTouchingSessionManagerWhenBearerHeaderIsMissingOrMalformed() {
        assertThat(handler.authenticate(context("/sys/admin/user-info", null)).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Basic sys_admin.1001.access-token")).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer sys_admin.1001.access-token extra")).isAuthenticated()).isFalse();

        verify(sessionManager, never()).validateAccessToken("sys_admin.1001.access-token");
    }

    private ServletAuthenticationContext context(String requestUri, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }
}
