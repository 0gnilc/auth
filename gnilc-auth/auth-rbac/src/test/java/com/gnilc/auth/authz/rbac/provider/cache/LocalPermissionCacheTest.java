package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

    @Test
    void repeatsInvalidationAfterTheDirtyReadProtectionDelay() {
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:1"));

        cache.resetUserPermissions(42L);
        assertThat(cache.loadUserPermissions(42L)).containsExactly(new Permission("user:42:2"));

        await().atMost(Duration.ofSeconds(7)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(cache.loadUserPermissions(42L))
                        .containsExactly(new Permission("user:42:3")));
        assertThat(loader.userLoads).hasValue(3);
    }

    @Test
    void concurrentMissesForOneUserPerformOnlyOneLoad() throws Exception {
        BlockingUserLoader blockingLoader = new BlockingUserLoader();
        LocalPermissionCache concurrentCache = new LocalPermissionCache(blockingLoader);
        ExecutorService callers = Executors.newFixedThreadPool(8);
        CountDownLatch callersReady = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<List<Permission>>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(callers.submit(() -> {
                    callersReady.countDown();
                    start.await();
                    return concurrentCache.loadUserPermissions(42L);
                }));
            }

            assertThat(callersReady.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(blockingLoader.loadStarted.await(2, TimeUnit.SECONDS)).isTrue();
            blockingLoader.releaseLoad.countDown();

            for (Future<List<Permission>> result : results) {
                assertThat(result.get(2, TimeUnit.SECONDS))
                        .containsExactly(new Permission("user:42:concurrent"));
            }
            assertThat(blockingLoader.userLoads).hasValue(1);
        } finally {
            blockingLoader.releaseLoad.countDown();
            callers.shutdownNow();
            concurrentCache.shutdownResetExecutor();
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

    private static final class BlockingUserLoader implements PermissionCacheLoader {
        private final AtomicInteger userLoads = new AtomicInteger();
        private final CountDownLatch loadStarted = new CountDownLatch(1);
        private final CountDownLatch releaseLoad = new CountDownLatch(1);

        @Override
        public List<TargetPermission> loadTargetPermissions() {
            return List.of();
        }

        @Override
        public List<Permission> loadUserPermissions(Long userId) {
            userLoads.incrementAndGet();
            loadStarted.countDown();
            try {
                if (!releaseLoad.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release the permission load");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Permission load was interrupted", exception);
            }
            return List.of(new Permission("user:" + userId + ":concurrent"));
        }

        @Override
        public List<Permission> loadPublicAccessPermissions() {
            return List.of();
        }
    }
}
