package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.context.AccessIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultServletAccessIdentityResolverHandlerTest {

    // TestCaseId: CORE-SERVLET-006
    @Test
    void resolvesAccessPrincipalFromHttpServletRequest() {
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(1001L, Map.of("source", "authn")));
        ServletRequestContext context = servletRequestContext(request);

        AccessIdentity identity = handler.handle(context);

        assertThat(handler.supports(context)).isTrue();
        assertThat(identity.getIdentifier()).isEqualTo("1001");
        assertThat(identity.getAttributes()).containsEntry("source", "authn");
        assertThat(identity.getAttributes()).containsEntry("principal", true);
    }

    // TestCaseId: CORE-SERVLET-007
    @Test
    void copiesPrincipalAttributes() {
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();
        Map<String, Object> attributes = Map.of("source", "authn");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of("1001", attributes));

        AccessIdentity identity = handler.handle(servletRequestContext(request));

        assertThat(identity.getAttributes()).isNotSameAs(attributes);
        assertThat(identity.getAttributes()).containsEntry("source", "authn");
        assertThat(identity.getAttributes()).containsEntry("principal", true);
    }

    // TestCaseId: CORE-SERVLET-008
    @Test
    void doesNotSupportGenericPrincipal() {
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(() -> "1001");

        assertThat(handler.supports(servletRequestContext(request))).isFalse();
    }

    // TestCaseId: CORE-SERVLET-009
    @Test
    void doesNotSupportHeaderOnlyIdentity() {
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Access-User-Id")).thenReturn("1001");
        when(request.getUserPrincipal()).thenReturn(null);

        assertThat(handler.supports(servletRequestContext(request))).isFalse();
    }

    // TestCaseId: CORE-SERVLET-010
    @Test
    void doesNotSupportBlankPrincipalIdentifier() {
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of("   "));

        assertThat(handler.supports(servletRequestContext(request))).isFalse();
    }

    private ServletRequestContext servletRequestContext(HttpServletRequest request) {
        FilterChain chain = (chainRequest, chainResponse) -> {
        };
        return new ServletRequestContext(request, new MockHttpServletResponse(), chain);
    }
}
