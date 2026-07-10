package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServletAccessContextTest {

    @Test
    void defaultAdapterProducesServletContextFromResolvers() {
        ServletAccessIdentityResolver identityResolver = mock(ServletAccessIdentityResolver.class);
        ServletAccessTargetResolver targetResolver = mock(ServletAccessTargetResolver.class);
        AccessIdentity identity = new AccessIdentity("user-1", Map.of("role", "admin"));
        AccessTarget target = new AccessTarget("/orders", "GET");
        ServletRequestContext source = new ServletRequestContext(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                mock(jakarta.servlet.FilterChain.class)
        );
        when(identityResolver.resolve(source)).thenReturn(identity);
        when(targetResolver.resolve(source)).thenReturn(target);

        AccessContext context = new DefaultServletAccessContextAdapter(identityResolver, targetResolver).adapt(source);

        assertThat(context.getEnvironment()).isSameAs(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity()).isSameAs(identity);
        assertThat(context.getTarget()).isSameAs(target);
    }

    @Test
    void defaultIdentityResolverUsesFirstSupportingHandlerThenFallsBackToAnonymous() {
        ServletAccessIdentityResolverHandler unsupported = mock(ServletAccessIdentityResolverHandler.class);
        ServletAccessIdentityResolverHandler supported = mock(ServletAccessIdentityResolverHandler.class);
        ServletRequestContext source = new ServletRequestContext(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                null
        );
        AccessIdentity resolved = new AccessIdentity("custom", Map.of());
        when(unsupported.supports(source)).thenReturn(false);
        when(supported.supports(source)).thenReturn(true);
        when(supported.handle(source)).thenReturn(resolved);
        DefaultServletAccessIdentityResolver resolver = new DefaultServletAccessIdentityResolver(
                java.util.List.of(unsupported, supported)
        );

        assertThat(resolver.resolve(source)).isSameAs(resolved);

        AccessIdentity anonymous = new DefaultServletAccessIdentityResolver(java.util.List.of()).resolve(source);
        assertThat(anonymous.getIdentifier()).isNull();
        assertThat(anonymous.getAttributes()).containsEntry("anonymous", true);
    }

    @Test
    void principalHandlerTranslatesAuthenticatedPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of("user-1", Map.of("tenant", "north")));
        ServletRequestContext source = new ServletRequestContext(request, new MockHttpServletResponse(), null);
        DefaultServletAccessIdentityResolverHandler handler = new DefaultServletAccessIdentityResolverHandler();

        AccessIdentity identity = handler.handle(source);

        assertThat(handler.supports(source)).isTrue();
        assertThat(identity.getIdentifier()).isEqualTo("user-1");
        assertThat(identity.getAttributes())
                .containsEntry("tenant", "north")
                .containsEntry("principal", true);
    }

    @Test
    void targetResolverRemovesContextPathAndKeepsMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/orders/7");
        request.setContextPath("/app");
        ServletRequestContext source = new ServletRequestContext(request, new MockHttpServletResponse(), null);

        AccessTarget target = new DefaultServletAccessTargetResolver().resolve(source);

        assertThat(target.getIdentifier()).isEqualTo("/orders/7");
        assertThat(target.getQualifier()).isEqualTo("POST");
    }
}
