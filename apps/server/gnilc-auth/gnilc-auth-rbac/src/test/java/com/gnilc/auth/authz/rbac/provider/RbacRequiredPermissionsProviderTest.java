package com.gnilc.auth.authz.rbac.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RbacRequiredPermissionsProviderTest {
    private final PermissionCache cache = mock(PermissionCache.class);
    private final RbacRequiredPermissionsProvider provider = new RbacRequiredPermissionsProvider();

    @BeforeEach
    void injectCache() {
        ReflectionTestUtils.setField(provider, "cache", cache);
    }

    @Test
    void usesAntPathMatchingAndDeduplicatesCodes() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/sys/**", "admin"),
                new TargetPermission("/sys/admin/*", "admin"),
                new TargetPermission("/public/**", "public")));

        assertThat(provider.provide(servletContext("/sys/admin/7")))
                .containsExactly(new Permission("admin"));
        assertThat(provider.provide(servletContext("/unknown"))).isEmpty();
    }

    @Test
    void ignoresNonServletEnvironment() {
        AccessContext context = new AccessContext(AccessEnvironment.of("worker"),
                new AccessIdentity("1", Map.of()), new AccessTarget("/sys", "GET"));

        assertThat(provider.supports(context)).isFalse();
        assertThat(provider.provide(context)).isEmpty();
    }

    private AccessContext servletContext(String path) {
        return new AccessContext(AccessEnvironment.SERVLET,
                new AccessIdentity("1", Map.of()), new AccessTarget(path, "GET"));
    }
}
