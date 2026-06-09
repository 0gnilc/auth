package com.gnilc.authz.rbac.provider.cache;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.TargetPermission;
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
    @Test
    void nullUserIdDoesNotLoadOrResetUserPermissions() {
        assertThat(cache.loadUserPermissions(null)).isEmpty();

        cache.resetUserPermissions(null);

        assertThat(loader.userLoadCount).hasValue(0);
    }

    /**
     * 可计数 loader 用于验证缓存是否真正复用加载结果。
     * 每次加载返回带序号的权限值，测试可以通过序号变化判断是否发生了重新加载。
     */
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
