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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacPermissionProviderTest {
    private final PermissionCache cache = mock(PermissionCache.class);
    private final RbacGrantedPermissionsProvider granted = new RbacGrantedPermissionsProvider();
    private final RbacRequiredPermissionsProvider required = new RbacRequiredPermissionsProvider();

    @BeforeEach
    void injectCache() {
        ReflectionTestUtils.setField(granted, "cache", cache);
        ReflectionTestUtils.setField(required, "cache", cache);
    }

    @Test
    void grantedPermissionsMergeUserAndPublicPermissions() {
        Permission own = new Permission("admin:read");
        Permission publicPermission = new Permission("public");
        when(cache.loadUserPermissions(42L)).thenReturn(List.of(own, publicPermission));
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(publicPermission));

        List<Permission> permissions = granted.provide(servletContext("42", "/admin"));

        assertThat(permissions).containsExactly(own, publicPermission);
        verify(cache).loadUserPermissions(42L);
    }

    @Test
    void nonNumericOrAnonymousIdentityReceivesOnlyPublicPermissions() {
        Permission publicPermission = new Permission("public");
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(publicPermission));

        assertThat(granted.provide(servletContext("service-account", "/public")))
                .containsExactly(publicPermission);
        assertThat(granted.provide(servletContext(null, "/public")))
                .containsExactly(publicPermission);
    }

    @Test
    void requiredPermissionsUseAntPathMatchingAndDeduplicateCodes() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/sys/**", "admin"),
                new TargetPermission("/sys/admin/*", "admin"),
                new TargetPermission("/public/**", "public")));

        assertThat(required.provide(servletContext("1", "/sys/admin/7")))
                .containsExactly(new Permission("admin"));
        assertThat(required.provide(servletContext("1", "/unknown"))).isEmpty();
    }

    @Test
    void providersIgnoreNonServletEnvironments() {
        AccessContext worker = new AccessContext(
                AccessEnvironment.of("worker"), new AccessIdentity("1", Map.of()),
                new AccessTarget("/sys", "GET"));

        assertThat(granted.supports(worker)).isFalse();
        assertThat(required.supports(worker)).isFalse();
        assertThat(granted.provide(worker)).isEmpty();
        assertThat(required.provide(worker)).isEmpty();
    }

    private AccessContext servletContext(String identity, String path) {
        return new AccessContext(AccessEnvironment.SERVLET,
                new AccessIdentity(identity, Map.of()), new AccessTarget(path, "GET"));
    }
}
