package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessIdentity;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessIdentityResolverTest {

    // TestCaseId: CORE-SERVLET-003
    @Test
    void resolveAnonymousIdentityWhenNoHandlerExists() {
        DefaultServletAccessIdentityResolver resolver = new DefaultServletAccessIdentityResolver(List.of());

        AccessIdentity identity = resolver.resolve(servletRequestContext());

        assertAnonymous(identity);
    }

    // TestCaseId: CORE-SERVLET-004
    @Test
    void useFirstSupportedServletHandler() {
        ServletAccessIdentityResolverHandler unsupported = new TestHandler(false, new AccessIdentity("1001", Map.of("source", "unsupported")));
        ServletAccessIdentityResolverHandler first = new TestHandler(true, new AccessIdentity("1002", Map.of("source", "first")));
        ServletAccessIdentityResolverHandler second = new TestHandler(true, new AccessIdentity("1003", Map.of("source", "second")));
        DefaultServletAccessIdentityResolver resolver = new DefaultServletAccessIdentityResolver(List.of(unsupported, first, second));

        AccessIdentity identity = resolver.resolve(servletRequestContext());

        assertThat(identity.getIdentifier()).isEqualTo("1002");
        assertThat(identity.getAttributes()).containsEntry("source", "first");
    }

    // TestCaseId: CORE-SERVLET-005
    @Test
    void returnHandlerIdentityAsIs() {
        AccessIdentity handlerIdentity = new AccessIdentity("ADMIN:1001", Map.of("source", "admin"));
        DefaultServletAccessIdentityResolver resolver = new DefaultServletAccessIdentityResolver(List.of(
                new TestHandler(true, handlerIdentity)
        ));

        AccessIdentity identity = resolver.resolve(servletRequestContext());

        assertThat(identity).isSameAs(handlerIdentity);
    }

    private void assertAnonymous(AccessIdentity identity) {
        assertThat(identity.getIdentifier()).isNull();
        assertThat(identity.getAttributes()).containsEntry("anonymous", true);
    }

    private ServletRequestContext servletRequestContext() {
        FilterChain chain = (request, response) -> {
        };
        return new ServletRequestContext(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
    }

    private static class TestHandler implements ServletAccessIdentityResolverHandler {
        private final boolean supports;
        private final AccessIdentity identity;

        private TestHandler(boolean supports, AccessIdentity identity) {
            this.supports = supports;
            this.identity = identity;
        }

        @Override
        public boolean supports(ServletRequestContext source) {
            return supports;
        }

        @Override
        public AccessIdentity handle(ServletRequestContext source) {
            return identity;
        }
    }
}
