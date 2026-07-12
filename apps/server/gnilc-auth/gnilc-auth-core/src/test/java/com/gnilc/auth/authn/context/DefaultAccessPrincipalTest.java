package com.gnilc.auth.authn.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAccessPrincipalTest {
    @Test
    void exposesAnImmutableAttributeSnapshot() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenant", "north");

        AccessPrincipal principal = DefaultAccessPrincipal.of(42L, attributes);
        attributes.put("tenant", "south");

        assertThat(principal.getIdentifier()).isEqualTo("42");
        assertThat(principal.getName()).isEqualTo("42");
        assertThat(principal.getAttributes()).containsEntry("tenant", "north");
    }
}
