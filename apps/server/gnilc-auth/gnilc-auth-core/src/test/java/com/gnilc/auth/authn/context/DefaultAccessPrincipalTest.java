package com.gnilc.auth.authn.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证调用方修改原始属性集合后，已建立认证主体的属性快照保持不变。 */
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
