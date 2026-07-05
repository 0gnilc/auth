package com.gnilc.auth.system.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSessionRedisCommandsTest {
    private final AdminSessionRedisCommands operations = new AdminSessionRedisCommands(null);

    // TestCaseId: SYS-REDIS-001
    @Test
    void buildsAccessAndRefreshTokenKeys() {
        assertThat(operations.accessKey(1001L, "sys_admin.1001.access-random"))
                .isEqualTo("sys:admin:at:1001:sys_admin.1001.access-random");
        assertThat(operations.refreshKey(1001L, "sys_admin.1001.refresh-random"))
                .isEqualTo("sys:admin:rt:1001:sys_admin.1001.refresh-random");
    }

    // TestCaseId: SYS-REDIS-002
    @Test
    void buildsUserCleanupPatterns() {
        assertThat(operations.accessPattern(1001L)).isEqualTo("sys:admin:at:1001:*");
        assertThat(operations.refreshPattern(1001L)).isEqualTo("sys:admin:rt:1001:*");
    }
}
