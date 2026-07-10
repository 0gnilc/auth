package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPermissionCacheTest {
    private CountingLoader loader;
    private LocalPermissionCache cache;

    @BeforeEach
    void setUp() {
        loader = new CountingLoader();
        cache = new LocalPermissionCache(loader);
    }

    @AfterEach
    void tearDown() {
        cache.shutdownResetExecutor();
    }

    @Test
    void cachesEachPermissionViewUntilItsTargetedReset() {
        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/1", "target:1"));
        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/1", "target:1"));
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:1"));
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:1"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:1"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:1"));

        assertThat(loader.targetLoads).hasValue(1);
        assertThat(loader.userLoads).hasValue(1);
        assertThat(loader.publicLoads).hasValue(1);

        cache.resetTargetPermissions();
        cache.resetUserPermissions(42L);
        cache.resetPublicAccessPermissions();

        assertThat(cache.loadTargetPermissions()).containsExactly(new TargetPermission("/target/2", "target:2"));
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:2"));
        assertThat(cache.loadPublicAccessPermissions()).containsExactly(new Permission("public:2"));
    }

    @Test
    void keepsUserCachesIndependent() {
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:1"));
        assertThat(cache.loadUserPermissions(84L)).containsExactly(new Permission("user:84:2"));

        cache.resetUserPermissions(42L);

        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:3"));
        assertThat(cache.loadUserPermissions(84L)).containsExactly(new Permission("user:84:2"));
        assertThat(loader.userLoads).hasValue(3);
    }

    @Test
    void resetAllInvalidatesEveryPermissionView() {
        cache.loadTargetPermissions();
        cache.loadUserPermissions(42L);
        cache.loadPublicAccessPermissions();

        cache.resetAll();

        cache.loadTargetPermissions();
        cache.loadUserPermissions(42L);
        cache.loadPublicAccessPermissions();
        assertThat(loader.targetLoads).hasValue(2);
        assertThat(loader.userLoads).hasValue(2);
        assertThat(loader.publicLoads).hasValue(2);
    }

    @Test
    void normalizesNullLoadsAndRejectsNullUserKeys() {
        LocalPermissionCache nullCache = new LocalPermissionCache(new NullLoader());
        try {
            assertThat(nullCache.loadTargetPermissions()).isEmpty();
            assertThat(nullCache.loadUserPermissions(42L)).isEmpty();
            assertThat(nullCache.loadPublicAccessPermissions()).isEmpty();
            assertThat(nullCache.loadUserPermissions(null)).isEmpty();
        } finally {
            nullCache.shutdownResetExecutor();
        }
    }

    private static final class CountingLoader implements PermissionCacheLoader {
        private final AtomicInteger targetLoads = new AtomicInteger();
        private final AtomicInteger userLoads = new AtomicInteger();
        private final AtomicInteger publicLoads = new AtomicInteger();

        @Override
        public List<TargetPermission> loadTargetPermissions() {
            int value = targetLoads.incrementAndGet();
            return List.of(new TargetPermission("/target/" + value, "target:" + value));
        }

        @Override
        public List<Permission> loadUserPermissions(Long userId) {
            return List.of(new Permission("user:" + userId + ":" + userLoads.incrementAndGet()));
        }

        @Override
        public List<Permission> loadPublicAccessPermissions() {
            return List.of(new Permission("public:" + publicLoads.incrementAndGet()));
        }
    }

    private static final class NullLoader implements PermissionCacheLoader {
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
}
