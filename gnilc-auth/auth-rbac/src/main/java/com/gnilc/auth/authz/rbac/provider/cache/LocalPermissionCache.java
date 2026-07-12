package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地权限缓存实现。
 * <p>
 * 该模块只负责本地缓存读写和延迟二次重置；重置策略和 Redis 传输由 reset 模块处理。
 */
public class LocalPermissionCache implements PermissionCache {
    private static final String TARGET_PERMISSIONS_CACHE_KEY = "TARGET_PERMISSIONS";
    private static final String PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY = "PUBLIC_ACCESS_PERMISSIONS";
    /**
     * 延迟二次重置时间，用于降低事务提交和并发读取造成的短暂脏缓存风险。
     */
    private static final Duration SECOND_RESET_DELAY = Duration.ofSeconds(5);

    /**
     * 目标权限缓存，供 {@code RbacRequiredPermissionsProvider} 匹配访问目标。
     */
    private final Cache<String, List<TargetPermission>> targetPermissionsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();
    /**
     * 用户权限缓存，key 为用户 ID。
     */
    private final Cache<Long, List<Permission>> userPermissionsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    /**
     * 公开访问权限缓存，对匿名和已识别身份都生效。
     */
    private final Cache<String, List<Permission>> publicAccessPermissionsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final ScheduledExecutorService resetExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "permission-cache-reset");
        thread.setDaemon(true);
        return thread;
    });

    private final PermissionCacheLoader permissionCacheLoader;

    /**
     * 创建本地权限缓存。
     *
     * @param permissionCacheLoader 权限数据加载器
     */
    public LocalPermissionCache(PermissionCacheLoader permissionCacheLoader) {
        this.permissionCacheLoader = permissionCacheLoader;
    }

    @Override
    public List<TargetPermission> loadTargetPermissions() {
        List<TargetPermission> cached = targetPermissionsCache.getIfPresent(TARGET_PERMISSIONS_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        synchronized (TARGET_PERMISSIONS_CACHE_KEY) {
            cached = targetPermissionsCache.getIfPresent(TARGET_PERMISSIONS_CACHE_KEY);
            if (cached != null) {
                return cached;
            }
            List<TargetPermission> loaded = Optional.ofNullable(permissionCacheLoader.loadTargetPermissions()).orElse(List.of());
            targetPermissionsCache.put(TARGET_PERMISSIONS_CACHE_KEY, loaded);
            return loaded;
        }
    }

    @Override
    public void resetTargetPermissions() {
        targetPermissionsCache.invalidate(TARGET_PERMISSIONS_CACHE_KEY);
        scheduleSecondReset(() -> targetPermissionsCache.invalidate(TARGET_PERMISSIONS_CACHE_KEY));
    }

    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Permission> cached = userPermissionsCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        synchronized (String.valueOf(userId).intern()) {
            cached = userPermissionsCache.getIfPresent(userId);
            if (cached != null) {
                return cached;
            }
            List<Permission> loaded = Optional.ofNullable(permissionCacheLoader.loadUserPermissions(userId)).orElse(List.of());
            userPermissionsCache.put(userId, loaded);
            return loaded;
        }
    }

    @Override
    public void resetUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }
        userPermissionsCache.invalidate(userId);
        scheduleSecondReset(() -> userPermissionsCache.invalidate(userId));
    }

    @Override
    public List<Permission> loadPublicAccessPermissions() {
        List<Permission> cached = publicAccessPermissionsCache.getIfPresent(PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        synchronized (PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY) {
            cached = publicAccessPermissionsCache.getIfPresent(PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY);
            if (cached != null) {
                return cached;
            }
            List<Permission> loaded = Optional.ofNullable(permissionCacheLoader.loadPublicAccessPermissions()).orElse(List.of());
            publicAccessPermissionsCache.put(PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY, loaded);
            return loaded;
        }
    }

    @Override
    public void resetPublicAccessPermissions() {
        publicAccessPermissionsCache.invalidate(PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY);
        scheduleSecondReset(() -> publicAccessPermissionsCache.invalidate(PUBLIC_ACCESS_PERMISSIONS_CACHE_KEY));
    }

    @Override
    public void resetAll() {
        targetPermissionsCache.invalidateAll();
        userPermissionsCache.invalidateAll();
        publicAccessPermissionsCache.invalidateAll();
        scheduleSecondReset(() -> {
            targetPermissionsCache.invalidateAll();
            userPermissionsCache.invalidateAll();
            publicAccessPermissionsCache.invalidateAll();
        });
    }

    /**
     * 关闭延迟重置线程池。
     */
    @PreDestroy
    public void shutdownResetExecutor() {
        resetExecutor.shutdown();
    }

    private void scheduleSecondReset(Runnable task) {
        resetExecutor.schedule(task, SECOND_RESET_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }
}
