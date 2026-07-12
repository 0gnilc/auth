package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DelegatingAccessIdentityResolverTest {
    @Test
    void usesFirstMatchThenFallback() {
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
}
