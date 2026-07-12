package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessContextTest {
    @Test
    void normalizesEnvironmentAndDefaultsToUnspecified() {
        assertThat(AccessEnvironment.of(" Servlet ")).isSameAs(AccessEnvironment.SERVLET);
        assertThat(AccessEnvironment.of("")).isSameAs(AccessEnvironment.UNSPECIFIED);
        assertThat(AccessEnvironment.of("WORKER").getIdentifier()).isEqualTo("worker");

        AccessContext context = new AccessContext(null, new AccessIdentity("1", null),
                new AccessTarget("job", "run"), Map.of("trace", "abc"));

        assertThat(context.getEnvironment()).isSameAs(AccessEnvironment.UNSPECIFIED);
        assertThat(context.getAttributes()).containsEntry("trace", "abc");
    }
}
