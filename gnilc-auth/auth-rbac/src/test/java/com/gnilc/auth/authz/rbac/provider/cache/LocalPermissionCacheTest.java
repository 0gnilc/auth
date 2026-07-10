package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPermissionCacheTest {
    private CountingPermissionCacheLoader loader;
    private LocalPermissionCache cache;

    /**
     * Sets up a fresh local permission cache before each test.
     */
    @BeforeEach
    void setUp() {
        loader = new CountingPermissionCacheLoader();
        cache = new LocalPermissionCache(loader);
    }

    /**
     * 目标权限第一次读取时应从 loader 加载，后续读取应直接复用本地缓存。
     * resetTargetPermissions 后缓存被清理，再次读取应触发 loader 重新加载新结果。
     */
    // TestCaseId: RBAC-CACHE-008
    @Test
    void cacheTargetPermissionsUntilReset() {
        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/1", "target:1"));
        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/1", "target:1"));
        assertThat(loader.targetLoadCount).hasValue(1);

        cache.resetTargetPermissions();

        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/2", "target:2"));
    }

    /**
     * 用户权限缓存按 userId 分组。
     * 同一用户连续读取只加载一次；重置该用户权限后，再读取同一 userId 应得到重新加载后的值。
     */
    // TestCaseId: RBAC-CACHE-009
    @Test
    void cacheUserPermissionsUntilReset() {
        assertThat(cache.loadUserPermissions(100L)).containsExactly(new Permission("user:1"));
        assertThat(cache.loadUserPermissions(100L)).containsExactly(new Permission("user:1"));
        assertThat(loader.userLoadCount).hasValue(1);

        cache.resetUserPermissions(100L);

        assertThat(cache.loadUserPermissions(100L)).containsExactly(new Permission("user:2"));
    }

    /**
     * 公开访问权限对所有身份共用，因此使用固定 key 缓存。
     * resetPublicAccessPermissions 后应清理公开访问权限缓存，使下一次读取重新加载。
     */
    // TestCaseId: RBAC-CACHE-010
    @Test
    void cachePublicAccessPermissionsUntilReset() {
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:1"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:1"));
        assertThat(loader.publicAccessLoadCount).hasValue(1);

        cache.resetPublicAccessPermissions();

        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:2"));
    }

    /**
     * resetAll 应同时清理目标权限、用户权限和公开访问权限缓存。
     * 三类缓存清理后再次读取，三个 loader 计数都应增加到 2。
     */
    // TestCaseId: RBAC-CACHE-011
    @Test
    void resetAllClearsAllCaches() {
        cache.loadTargetPermissions();
        cache.loadUserPermissions(100L);
        cache.loadPublicAccessPermissions();

        cache.resetAll();

        cache.loadTargetPermissions();
        cache.loadUserPermissions(100L);
        cache.loadPublicAccessPermissions();
        assertThat(loader.targetLoadCount).hasValue(2);
        assertThat(loader.userLoadCount).hasValue(2);
        assertThat(loader.publicAccessLoadCount).hasValue(2);
    }

    /**
     * null userId 表示无法定位具体用户权限缓存。
     * 读取时返回空集合，重置时直接忽略，避免无意义 loader 调用和空 key 缓存写入。
     */
    // TestCaseId: RBAC-CACHE-012
    @Test
    void nullUserIdDoesNotLoadOrResetUserPermissions() {
        assertThat(cache.loadUserPermissions(null)).isEmpty();

        cache.resetUserPermissions(null);

        assertThat(loader.userLoadCount).hasValue(0);
    }

    /**
     * loader 返回 null 时应按空集合缓存，避免 provider 链路收到空指针。
     */
    // TestCaseId: RBAC-CACHE-013
    @Test
    void treatNullLoaderResultsAsEmptyLists() {
        NullPermissionCacheLoader nullLoader = new NullPermissionCacheLoader();
        LocalPermissionCache nullCache = new LocalPermissionCache(nullLoader);

        assertThat(nullCache.loadTargetPermissions()).isEmpty();
        assertThat(nullCache.loadUserPermissions(100L)).isEmpty();
        assertThat(nullCache.loadPublicAccessPermissions()).isEmpty();
    }

    /**
     * 并发读取同一用户权限时，intern lock 应确保同一 userId 只发生一次实际加载。
     */
    // TestCaseId: RBAC-CACHE-014
    @Test
    void loadSameUserPermissionsOnceWhenCalledConcurrently() throws Exception {
        Thread first = new Thread(() -> cache.loadUserPermissions(100L));
        Thread second = new Thread(() -> cache.loadUserPermissions(100L));

        first.start();
        second.start();
        first.join();
        second.join();

        assertThat(loader.userLoadCount).hasValue(1);
        assertThat(cache.loadUserPermissions(100L)).containsExactly(new Permission("user:1"));
    }

    /**
     * 空 loader 用于验证缓存对 null 加载结果的防御性处理。
     */
    private static class NullPermissionCacheLoader implements PermissionCacheLoader {
        @Override
        public List<TargetPermission> loadTargetPermissions() {
            return null;
        }

        @Override
        public List<Permission> loadUserPermissions(Long userId) {
            return null;
        }

        @Override
        public List<Permission> loadPublicAccessPermissions() {
            return null;
        }
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
