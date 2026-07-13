package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPermissionCacheTest {
    @Test
    void delegatesReadsWithoutState() {
        PermissionCacheLoader loader = mock(PermissionCacheLoader.class);
        TargetPermission target = new TargetPermission("/x", "read");
        when(loader.loadTargetPermissions()).thenReturn(List.of(target));

        assertThat(new DefaultPermissionCache(loader).loadTargetPermissions()).containsExactly(target);
    }
}
