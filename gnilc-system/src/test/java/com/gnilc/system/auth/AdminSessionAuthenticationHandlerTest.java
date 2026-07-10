package com.gnilc.system.auth;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.system.session.AdminSessionManager;
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

    @Test
    void supportsAdminBearerTokensRegardlessOfHeaderCaseOrRequestPath() {
        when(sessionManager.supportsAccessToken("sys_admin.1001.access-token")).thenReturn(true);

        assertThat(handler.supports(context("/anything", "bearer sys_admin.1001.access-token"))).isTrue();
        verify(sessionManager).supportsAccessToken("sys_admin.1001.access-token");
    }

    @Test
    void ignoresMissingNonBearerAndForeignBearerCredentials() {
        when(sessionManager.supportsAccessToken("foreign-token")).thenReturn(false);

        assertThat(handler.supports(context("/sys/admin/user-info", null))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", "Basic credentials"))).isFalse();
        assertThat(handler.supports(context("/sys/admin/user-info", "Bearer foreign-token"))).isFalse();
    }

    @Test
    void authenticatesAStoredAccessTokenAsTheAdminUser() {
        when(sessionManager.validateAccessToken("sys_admin.1001.access-token")).thenReturn(1001L);

        AuthenticationResult result = handler.authenticate(
                context("/sys/admin/user-info", "Bearer sys_admin.1001.access-token"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(DefaultAccessPrincipal.class);
        assertThat(result.getPrincipal().getName()).isEqualTo("1001");
        assertThat(result.getPrincipal().getAttributes()).isEmpty();
    }

    @Test
    void rejectsExpiredOrUnknownAccessTokens() {
        when(sessionManager.validateAccessToken("expired-token")).thenReturn(null);

        AuthenticationResult result = handler.authenticate(
                context("/sys/admin/user-info", "Bearer expired-token"));

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getReason()).isEqualTo("invalid access token");
    }

    @Test
    void rejectsMalformedBearerValuesBeforeSessionValidation() {
        assertThat(handler.authenticate(context("/sys/admin/user-info", null)).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer")).isAuthenticated()).isFalse();
        assertThat(handler.authenticate(context("/sys/admin/user-info", "Bearer token extra")).isAuthenticated()).isFalse();

        verify(sessionManager, never()).validateAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    private ServletAuthenticationContext context(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }
}
