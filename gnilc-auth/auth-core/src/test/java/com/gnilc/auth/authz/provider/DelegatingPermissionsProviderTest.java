package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelegatingPermissionsProviderTest {
    private static final Permission READ = new Permission("read");
    private static final Permission WRITE = new Permission("write");

    @Test
    void grantedProviderCombinesSupportedProvidersAndRemovesDuplicates() {
        AccessContext context = mock(AccessContext.class);
        GrantedPermissionsProvider supported = mock(GrantedPermissionsProvider.class);
        GrantedPermissionsProvider nullable = mock(GrantedPermissionsProvider.class);
        GrantedPermissionsProvider unsupported = mock(GrantedPermissionsProvider.class);
        when(supported.supports(context)).thenReturn(true);
        when(supported.provide(context)).thenReturn(List.of(READ, WRITE, READ));
        when(nullable.supports(context)).thenReturn(true);
        when(nullable.provide(context)).thenReturn(null);
        when(unsupported.supports(context)).thenReturn(false);

        List<Permission> result = new DelegatingGrantedPermissionsProvider(
                linkedSet(supported, nullable, unsupported)
        ).provide(context);

        assertThat(result).containsExactly(READ, WRITE);
        verify(unsupported, never()).provide(context);
    }

    @Test
    void requiredProviderCombinesSupportedProvidersAndRemovesDuplicates() {
        AccessContext context = mock(AccessContext.class);
        RequiredPermissionsProvider first = mock(RequiredPermissionsProvider.class);
        RequiredPermissionsProvider nullable = mock(RequiredPermissionsProvider.class);
        RequiredPermissionsProvider second = mock(RequiredPermissionsProvider.class);
        RequiredPermissionsProvider unsupported = mock(RequiredPermissionsProvider.class);
        when(first.supports(context)).thenReturn(true);
        when(first.provide(context)).thenReturn(List.of(READ));
        when(nullable.supports(context)).thenReturn(true);
        when(nullable.provide(context)).thenReturn(null);
        when(second.supports(context)).thenReturn(true);
        when(second.provide(context)).thenReturn(List.of(READ, WRITE));
        when(unsupported.supports(context)).thenReturn(false);

        List<Permission> result = new DelegatingRequiredPermissionsProvider(
                linkedSet(first, nullable, second, unsupported)
        ).provide(context);

        assertThat(result).containsExactly(READ, WRITE);
        verify(unsupported, never()).provide(context);
    }

    @Test
    void constructorsRejectMissingProviders() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DelegatingGrantedPermissionsProvider(Set.of()))
                .withMessage("providers is Empty!");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DelegatingRequiredPermissionsProvider(null))
                .withMessage("providers is Empty!");
    }

    @SafeVarargs
    private static <T> Set<T> linkedSet(T... elements) {
        return new LinkedHashSet<>(List.of(elements));
    }
}
