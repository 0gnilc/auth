package com.gnilc.system.auth;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import com.gnilc.system.session.AdminSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemAuthAdapterTest {
    private final AdminSessionManager sessions = mock(AdminSessionManager.class);
    private final AdminSessionAuthenticationHandler authentication =
            new AdminSessionAuthenticationHandler(sessions);

    @Test
    void bearerTokenSupportIsNamespacedBySessionManager() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(sessions.supportsAccessToken("token")).thenReturn(true);

        assertThat(authentication.supports(context(request))).isTrue();

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        assertThat(authentication.supports(context(request))).isFalse();
    }

    @Test
    void validBearerTokenCreatesPrincipalAndInvalidTokenFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        when(sessions.validateAccessToken("valid")).thenReturn(12L);

        AuthenticationResult valid = authentication.authenticate(context(request));

        assertThat(valid.isAuthenticated()).isTrue();
        assertThat(valid.getPrincipal().getIdentifier()).isEqualTo("12");

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        when(sessions.validateAccessToken("invalid")).thenReturn(null);
        assertThat(authentication.authenticate(context(request)).isAuthenticated()).isFalse();
    }

    @Test
    void deniedHandlerWritesJson403OnlyForOpenServletResponse() throws Exception {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAccessDeniedContext deniedContext = new ServletAccessDeniedContext(
                new MockHttpServletRequest(), response, (req, res) -> { });
        AccessContext access = new AccessContext(
                new AccessIdentity("1", Map.of()), new AccessTarget("/private", "GET"));

        assertThat(handler.supports(access, deniedContext)).isTrue();
        handler.handle(access, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":20003", "\"error\":\"access denied\"");
        assertThat(handler.supports(access, new AccessDeniedContext() { })).isFalse();
    }

    private ServletAuthenticationContext context(MockHttpServletRequest request) {
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }
}
