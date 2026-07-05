package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DelegatingAccessIdentityResolverTest {

    // TestCaseId: CORE-AUTHZ-014
    @Test
    void useFirstSupportedHandler() {
        AccessIdentityResolverHandler<String> unsupported = new TestHandler(false, new AccessIdentity("1001", Map.of("source", "unsupported")));
        AccessIdentityResolverHandler<String> first = new TestHandler(true, new AccessIdentity("1002", Map.of("source", "first")));
        AccessIdentityResolverHandler<String> second = new TestHandler(true, new AccessIdentity("1003", Map.of("source", "second")));
        DelegatingAccessIdentityResolver<String> resolver = new DelegatingAccessIdentityResolver<>(
                List.of(unsupported, first, second),
                source -> new AccessIdentity(null, Map.of("anonymous", true))
        );

        AccessIdentity identity = resolver.resolve("request");

        assertThat(identity.getIdentifier()).isEqualTo("1002");
        assertThat(identity.getAttributes()).containsEntry("source", "first");
    }

    // TestCaseId: CORE-AUTHZ-015
    @Test
    void returnHandlerIdentityAsIs() {
        AccessIdentity handlerIdentity = new AccessIdentity("ADMIN:1001", Map.of("source", "admin"));
        DelegatingAccessIdentityResolver<String> resolver = new DelegatingAccessIdentityResolver<>(
                List.of(new TestHandler(true, handlerIdentity)),
                source -> new AccessIdentity(null, Map.of("anonymous", true))
        );

        AccessIdentity identity = resolver.resolve("request");

        assertThat(identity).isSameAs(handlerIdentity);
    }

    // TestCaseId: CORE-AUTHZ-016
    @Test
    void useFallbackResolverWhenNoHandlerSupportsSource() {
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        AccessIdentity fallbackIdentity = new AccessIdentity(null, Map.of("anonymous", true));
        DelegatingAccessIdentityResolver<String> resolver = new DelegatingAccessIdentityResolver<>(
                List.of(new TestHandler(false, new AccessIdentity("1001", Map.of()))),
                source -> {
                    fallbackCalled.set(true);
                    return fallbackIdentity;
                }
        );

        AccessIdentity identity = resolver.resolve("request");

        assertThat(identity).isSameAs(fallbackIdentity);
        assertThat(fallbackCalled).isTrue();
    }

    // TestCaseId: CORE-AUTHZ-017
    @Test
    void useFallbackResolverWhenHandlersAreEmpty() {
        AccessIdentity fallbackIdentity = new AccessIdentity(null, Map.of("anonymous", true));
        DelegatingAccessIdentityResolver<String> resolver = new DelegatingAccessIdentityResolver<>(
                List.of(),
                source -> fallbackIdentity
        );

        AccessIdentity identity = resolver.resolve("request");

        assertThat(identity).isSameAs(fallbackIdentity);
    }

    // TestCaseId: CORE-AUTHZ-018
    @Test
    void requireFallbackResolver() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DelegatingAccessIdentityResolver<>(List.of(), null))
                .withMessage("fallbackResolver must not be null");
    }

    // TestCaseId: CORE-AUTHZ-019
    @Test
    void keepResolverPropertiesAsFinalFields() throws NoSuchFieldException {
        Field handlers = DelegatingAccessIdentityResolver.class.getDeclaredField("handlers");
        Field fallbackResolver = DelegatingAccessIdentityResolver.class.getDeclaredField("fallbackResolver");

        assertThat(Modifier.isFinal(handlers.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(fallbackResolver.getModifiers())).isTrue();
    }

    private static class TestHandler implements AccessIdentityResolverHandler<String> {
        private final boolean supports;
        private final AccessIdentity identity;

        private TestHandler(boolean supports, AccessIdentity identity) {
            this.supports = supports;
            this.identity = identity;
        }

        @Override
        public boolean supports(String source) {
            return supports;
        }

        @Override
        public AccessIdentity handle(String source) {
            return identity;
        }
    }
}
