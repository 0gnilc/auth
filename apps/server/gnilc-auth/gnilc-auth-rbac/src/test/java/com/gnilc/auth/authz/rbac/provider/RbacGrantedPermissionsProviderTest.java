package com.gnilc.auth.authz.rbac.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacGrantedPermissionsProviderTest {
    private final PermissionCacheService cacheService = mock(PermissionCacheService.class);
    private final RbacGrantedPermissionsProvider provider = new RbacGrantedPermissionsProvider();

    @BeforeEach
    void injectCache() {
        ReflectionTestUtils.setField(provider, "cacheService", cacheService);
    }

    @Test
    void mergesUserAndPublicPermissions() {
        Permission own = new Permission("admin:read");
        Permission publicPermission = new Permission("public");
        when(cacheService.loadUserPermissions(42L)).thenReturn(List.of(own, publicPermission));
        when(cacheService.loadPublicAccessPermissions()).thenReturn(List.of(publicPermission));

        assertThat(provider.provide(servletContext("42", "/admin")))
                .containsExactly(own, publicPermission);
        verify(cacheService).loadUserPermissions(42L);
    }

    @Test
    void nonNumericOrAnonymousIdentityReceivesOnlyPublicPermissions() {
        Permission permission = new Permission("public");
        when(cacheService.loadPublicAccessPermissions()).thenReturn(List.of(permission));

        assertThat(provider.provide(servletContext("service-account", "/public")))
                .containsExactly(permission);
        assertThat(provider.provide(servletContext(null, "/public"))).containsExactly(permission);
    }

    @Test
    void ignoresNonServletEnvironment() {
        AccessContext context = new AccessContext(AccessEnvironment.of("worker"),
                new AccessIdentity("1", Map.of()), new AccessTarget("/sys", "GET"));

        assertThat(provider.supports(context)).isFalse();
        assertThat(provider.provide(context)).isEmpty();
    }

    private AccessContext servletContext(String identity, String path) {
        return new AccessContext(AccessEnvironment.SERVLET,
                new AccessIdentity(identity, Map.of()), new AccessTarget(path, "GET"));
    }
}
