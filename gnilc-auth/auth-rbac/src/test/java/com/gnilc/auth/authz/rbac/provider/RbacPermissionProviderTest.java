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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RbacPermissionProviderTest {
    private PermissionCache cache;
    private RbacGrantedPermissionsProvider grantedProvider;
    private RbacRequiredPermissionsProvider requiredProvider;

    @BeforeEach
    void setUp() {
        cache = mock(PermissionCache.class);
        grantedProvider = new RbacGrantedPermissionsProvider();
        requiredProvider = new RbacRequiredPermissionsProvider();
        ReflectionTestUtils.setField(grantedProvider, "cache", cache);
        ReflectionTestUtils.setField(requiredProvider, "cache", cache);
    }

    @Test
    void mergesUserAndPublicGrantsAndRemovesDuplicates() {
        when(cache.loadUserPermissions(42L)).thenReturn(List.of(
                new Permission("account:read"),
                new Permission("shared:read")
        ));
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(
                new Permission("shared:read"),
                new Permission("health:read")
        ));

        assertThat(grantedProvider.provide(context(AccessEnvironment.SERVLET, "42", "/accounts/42", "GET")))
                .containsExactly(
                        new Permission("account:read"),
                        new Permission("shared:read"),
                        new Permission("health:read")
                );
    }

    @Test
    void anonymousOrNonNumericIdentitiesReceiveOnlyPublicGrants() {
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("health:read")));

        assertThat(grantedProvider.provide(context(AccessEnvironment.SERVLET, null, "/health", "GET")))
                .containsExactly(new Permission("health:read"));
        assertThat(grantedProvider.provide(context(AccessEnvironment.SERVLET, "admin", "/health", "GET")))
                .containsExactly(new Permission("health:read"));
        verify(cache, never()).loadUserPermissions(anyLong());
    }

    @Test
    void resolvesRequiredPermissionsWithAntPathSemanticsAndIgnoresQualifier() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/accounts/**", "account:read"),
                new TargetPermission("/accounts/{id}", "account:read"),
                new TargetPermission("/roles/**", "role:read")
        ));

        assertThat(requiredProvider.provide(context(
                AccessEnvironment.SERVLET, "42", "/accounts/42", "DELETE")))
                .containsExactly(new Permission("account:read"));
    }

    @Test
    void providersOnlyParticipateInServletAccesses() {
        AccessContext messageContext = context(
                AccessEnvironment.of("message"), "42", "/accounts/42", "GET");

        assertThat(grantedProvider.supports(messageContext)).isFalse();
        assertThat(requiredProvider.supports(messageContext)).isFalse();
        assertThat(grantedProvider.provide(messageContext)).isEmpty();
        assertThat(requiredProvider.provide(messageContext)).isEmpty();
        verifyNoInteractions(cache);
    }

    private AccessContext context(AccessEnvironment environment,
                                  String identity,
                                  String target,
                                  String qualifier) {
        return new AccessContext(
                environment,
                new AccessIdentity(identity, Map.of()),
                new AccessTarget(target, qualifier, Map.of()),
                Map.of()
        );
    }
}
