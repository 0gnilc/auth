package com.gnilc.auth.authz;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessIdentityResolverHandler;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.context.DelegatingAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolver;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessIdentityResolverHandler;
import com.gnilc.auth.authz.servlet.context.DefaultServletAccessTargetResolver;
import com.gnilc.auth.authz.servlet.context.ServletRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAccessContextTest {
    @Test
    void environmentsAreNormalizedAndContextDefaultsToUnspecified() {
        assertThat(AccessEnvironment.of(" Servlet ")).isSameAs(AccessEnvironment.SERVLET);
        assertThat(AccessEnvironment.of("")).isSameAs(AccessEnvironment.UNSPECIFIED);
        assertThat(AccessEnvironment.of("WORKER").getIdentifier()).isEqualTo("worker");

        AccessContext context = new AccessContext(null, new AccessIdentity("1", null),
                new AccessTarget("job", "run"), Map.of("trace", "abc"));

        assertThat(context.getEnvironment()).isSameAs(AccessEnvironment.UNSPECIFIED);
        assertThat(context.getAttributes()).containsEntry("trace", "abc");
    }

    @Test
    void delegatingIdentityResolverUsesFirstMatchThenFallback() {
        AccessIdentityResolverHandler<String> first = new AccessIdentityResolverHandler<>() {
            @Override
            public boolean supports(String source) {
                return source.startsWith("user:");
            }

            @Override
            public AccessIdentity handle(String source) {
                return new AccessIdentity(source.substring(5), Map.of());
            }
        };
        DelegatingAccessIdentityResolver<String> resolver = new DelegatingAccessIdentityResolver<>(
                List.of(first), source -> new AccessIdentity(null, Map.of("anonymous", true)));

        assertThat(resolver.resolve("user:17").getIdentifier()).isEqualTo("17");
        assertThat(resolver.resolve("public").getAttributes()).containsEntry("anonymous", true);
    }

    @Test
    void servletAdapterExtractsPrincipalAndContextRelativeTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders/7");
        request.setContextPath("/api");
        request.setUserPrincipal(DefaultAccessPrincipal.of("42", Map.of("tenant", "north")));
        ServletRequestContext source =
                new ServletRequestContext(request, new MockHttpServletResponse(), (req, res) -> { });
        DefaultServletAccessIdentityResolver identityResolver = new DefaultServletAccessIdentityResolver(
                List.of(new DefaultServletAccessIdentityResolverHandler()));
        DefaultServletAccessContextAdapter adapter = new DefaultServletAccessContextAdapter(
                identityResolver, new DefaultServletAccessTargetResolver());

        AccessContext context = adapter.adapt(source);

        assertThat(context.getEnvironment()).isSameAs(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity().getIdentifier()).isEqualTo("42");
        assertThat(context.getIdentity().getAttributes())
                .containsEntry("tenant", "north")
                .containsEntry("principal", true);
        assertThat(context.getTarget().getIdentifier()).isEqualTo("/orders/7");
        assertThat(context.getTarget().getQualifier()).isEqualTo("POST");
    }

    @Test
    void servletIdentityFallsBackToAnonymousWithoutPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        ServletRequestContext source =
                new ServletRequestContext(request, new MockHttpServletResponse(), (req, res) -> { });

        AccessIdentity identity = new DefaultServletAccessIdentityResolver(
                List.of(new DefaultServletAccessIdentityResolverHandler())).resolve(source);

        assertThat(identity.getIdentifier()).isNull();
        assertThat(identity.getAttributes()).containsEntry("anonymous", true);
    }
}
