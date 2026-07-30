package com.gnilc.auth.authz.rbac.provider.cache;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionCacheResetExecutorTest {
    @Test
    void routesEveryCommandType() {
        PermissionCacheService cacheService = mock(PermissionCacheService.class);
        PermissionCacheResetExecutor executor = new PermissionCacheResetExecutor(cacheService);

        executor.execute(PermissionCacheResetCommand.targetPermissions());
        executor.execute(PermissionCacheResetCommand.publicAccessPermissions());
        executor.execute(PermissionCacheResetCommand.userPermissions(5L));
        executor.execute(PermissionCacheResetCommand.all());
        executor.execute(null);

        verify(cacheService).resetTargetPermissions();
        verify(cacheService).resetPublicAccessPermissions();
        verify(cacheService).resetUserPermissions(5L);
        verify(cacheService).resetAll();
    }
}
