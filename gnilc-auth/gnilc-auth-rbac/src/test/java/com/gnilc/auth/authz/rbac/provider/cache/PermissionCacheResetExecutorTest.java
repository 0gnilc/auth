package com.gnilc.auth.authz.rbac.provider.cache;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionCacheResetExecutorTest {
    @Test
    void routesEveryCommandType() {
        PermissionCache cache = mock(PermissionCache.class);
        PermissionCacheResetExecutor executor = new PermissionCacheResetExecutor(cache);

        executor.execute(PermissionCacheResetCommand.targetPermissions());
        executor.execute(PermissionCacheResetCommand.publicAccessPermissions());
        executor.execute(PermissionCacheResetCommand.userPermissions(5L));
        executor.execute(PermissionCacheResetCommand.all());
        executor.execute(null);

        verify(cache).resetTargetPermissions();
        verify(cache).resetPublicAccessPermissions();
        verify(cache).resetUserPermissions(5L);
        verify(cache).resetAll();
    }
}
