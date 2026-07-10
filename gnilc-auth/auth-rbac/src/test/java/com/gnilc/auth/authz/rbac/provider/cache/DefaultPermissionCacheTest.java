package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPermissionCacheTest {

    // 默认缓存无本地状态，每次读取目标权限都应委托 loader。
    // TestCaseId: RBAC-CACHE-001
    @Test
    void delegateTargetPermissionsToLoaderEveryTime() {
        CountingPermissionCacheLoader loader = new CountingPermissionCacheLoader();
        DefaultPermissionCache cache = new DefaultPermissionCache(loader);

        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/1", "target:1"));
        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/2", "target:2"));
        assertThat(loader.targetLoadCount).hasValue(2);
    }

    // 默认缓存的 reset 操作不保存状态，也不应触发额外 loader 读取。
    // TestCaseId: RBAC-CACHE-002
    @Test
    void resetOperationsAreNoOps() {
        CountingPermissionCacheLoader loader = new CountingPermissionCacheLoader();
        DefaultPermissionCache cache = new DefaultPermissionCache(loader);

        cache.resetTargetPermissions();
        cache.resetUserPermissions(1001L);
        cache.resetPublicAccessPermissions();
        cache.resetAll();

        assertThat(loader.targetLoadCount).hasValue(0);
        assertThat(loader.userLoadCount).hasValue(0);
        assertThat(loader.publicAccessLoadCount).hasValue(0);
    }

    // 用户权限和公开访问权限也保持无状态委托语义。
    // TestCaseId: RBAC-CACHE-003
    @Test
    void delegateUserAndPublicAccessPermissionsToLoaderEveryTime() {
        CountingPermissionCacheLoader loader = new CountingPermissionCacheLoader();
        DefaultPermissionCache cache = new DefaultPermissionCache(loader);

        assertThat(cache.loadUserPermissions(1001L)).containsExactly(new Permission("user:1"));
        assertThat(cache.loadUserPermissions(1001L)).containsExactly(new Permission("user:2"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:1"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:2"));

        assertThat(loader.userLoadCount).hasValue(2);
        assertThat(loader.publicAccessLoadCount).hasValue(2);
    }

    private static class CountingPermissionCacheLoader implements PermissionCacheLoader {
        private final AtomicInteger targetLoadCount = new AtomicInteger();
        private final AtomicInteger userLoadCount = new AtomicInteger();
        private final AtomicInteger publicAccessLoadCount = new AtomicInteger();

        @Override
        public List<TargetPermission> loadTargetPermissions() {
            int value = targetLoadCount.incrementAndGet();
            return List.of(new TargetPermission("/target/" + value, "target:" + value));
        }

        @Override
        public List<Permission> loadUserPermissions(Long userId) {
            int value = userLoadCount.incrementAndGet();
            return List.of(new Permission("user:" + value));
        }

        @Override
        public List<Permission> loadPublicAccessPermissions() {
            int value = publicAccessLoadCount.incrementAndGet();
            return List.of(new Permission("public:" + value));
        }
    }
}
