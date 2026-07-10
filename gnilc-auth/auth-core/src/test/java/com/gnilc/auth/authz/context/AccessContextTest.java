package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessContextTest {

    // TestCaseId: CORE-AUTHZ-001
    @Test
    void createAccessContextWithEnvironmentAndAttributes() {
        AccessIdentity identity = new AccessIdentity("1001", Map.of());
        AccessTarget target = new AccessTarget("/admin/users", null, Map.of());

        AccessContext context = new AccessContext(
                AccessEnvironment.SERVLET,
                identity,
                target,
                Map.of("source", "test")
        );

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity()).isSameAs(identity);
        assertThat(context.getTarget()).isSameAs(target);
        assertThat(context.getAttributes()).containsEntry("source", "test");
    }

    // TestCaseId: CORE-AUTHZ-002
    @Test
    void defaultAccessEnvironmentToUnspecifiedForLegacyConstructors() {
        AccessContext context = new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of("source", "test")
        );

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.UNSPECIFIED);
        assertThat(context.getAttributes()).containsEntry("source", "test");
    }

    // TestCaseId: CORE-AUTHZ-003
    @Test
    void defaultNullAccessEnvironmentToUnspecified() {
        AccessContext context = new AccessContext(
                null,
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of()
        );

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.UNSPECIFIED);
    }

    // TestCaseId: CORE-AUTHZ-004
    @Test
    void treatNullAttributesAsEmpty() {
        AccessContext context = new AccessContext(
                AccessEnvironment.SERVLET,
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                null
        );

        assertThat(context.getAttributes()).isEmpty();
    }

    // TestCaseId: CORE-AUTHZ-005
    @Test
    void copyAttributesOnConstruction() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("source", "test");

        AccessContext context = new AccessContext(
                AccessEnvironment.SERVLET,
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                attributes
        );
        attributes.put("source", "changed");
        attributes.put("late", true);

        assertThat(context.getAttributes())
                .containsEntry("source", "test")
                .doesNotContainKey("late");
    }
}
