package com.gnilc.authz.rbac.cache;

import com.gnilc.authz.rbac.service.event.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.service.event.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Configuration
public class LocalPermissionCache extends DefaultPermissionCache {
    private final String ALL_RESOURCE_PERMISSIONS = "ALL_RESOURCE_PERMISSIONS";
    private final Cache<String, List<ResourcePermission>> ALL_RESOURCE_PERMISSIONS_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();

    private final static String USER_PERMISSIONS_CACHE_LOCK_PREFIX = "USER_PERMISSIONS_CACHE_LOCK_";
    private final Cache<Long, List<Permission>> USER_PERMISSIONS_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final static String EXPOSED_PERMISSIONS = "EXPOSED_PERMISSIONS";
    private final Cache<String, List<Permission>> EXPOSED_PERMISSIONS_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private static final ScheduledExecutorService deleteCacheService = Executors.newSingleThreadScheduledExecutor();

    private final static String PERMISSION_EVENT_CHANNEL = "PERMISSION_EVENT";
    private final static String ROLE_EVENT_CHANNEL = "ROLE_EVENT";
    private final static String ROLE_PERMISSION_EVENT_CHANNEL = "ROLE_PERMISSION_EVENT";
    private final static String USER_ROLE_EVENT_CHANNEL = "USER_ROLE_EVENT";
    private final static String USER_EVENT_CHANNEL = "USER_EVENT";
    private final static String CLEAR_EVENT_CHANNEL = "CLEAR_EVENT";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<ResourcePermission> loadAllResourcePermissions() {
        List<ResourcePermission> rps = ALL_RESOURCE_PERMISSIONS_CACHE.getIfPresent(ALL_RESOURCE_PERMISSIONS);
        if (!CollectionUtils.isEmpty(rps)) {
            return rps;
        }
        synchronized (ALL_RESOURCE_PERMISSIONS) {
            rps = ALL_RESOURCE_PERMISSIONS_CACHE.getIfPresent(ALL_RESOURCE_PERMISSIONS);
            if (!CollectionUtils.isEmpty(rps)) {
                return rps;
            }
            rps = super.loadAllResourcePermissions();
            rps = Optional.ofNullable(rps).orElse(List.of());
            ALL_RESOURCE_PERMISSIONS_CACHE.put(ALL_RESOURCE_PERMISSIONS, rps);
            return rps;
        }
    }

    @Override
    public void refreshAllResourcePermissions() {
        ALL_RESOURCE_PERMISSIONS_CACHE.put(ALL_RESOURCE_PERMISSIONS, List.of());
        deleteCacheService.schedule(() -> ALL_RESOURCE_PERMISSIONS_CACHE.put(ALL_RESOURCE_PERMISSIONS, List.of()),
                5, TimeUnit.SECONDS);
    }

    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Permission> ps = USER_PERMISSIONS_CACHE.getIfPresent(userId);
        if (!CollectionUtils.isEmpty(ps)) {
            return ps;
        }
        synchronized ((USER_PERMISSIONS_CACHE_LOCK_PREFIX + userId).intern()) {
            ps = USER_PERMISSIONS_CACHE.getIfPresent(userId);
            if (!CollectionUtils.isEmpty(ps)) {
                return ps;
            }
            ps = super.loadUserPermissions(userId);
            ps = Optional.ofNullable(ps).orElse(List.of());
            USER_PERMISSIONS_CACHE.put(userId, ps);
            return ps;
        }
    }

    @Override
    public void refreshUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }
        USER_PERMISSIONS_CACHE.put(userId, List.of());
        deleteCacheService.schedule(() -> USER_PERMISSIONS_CACHE.put(userId, List.of()),
                5, TimeUnit.SECONDS);
    }

    @Override
    public List<Permission> loadExposedPermissions() {
        List<Permission> eps = EXPOSED_PERMISSIONS_CACHE.getIfPresent(EXPOSED_PERMISSIONS);
        if (!CollectionUtils.isEmpty(eps)) {
            return eps;
        }
        synchronized (EXPOSED_PERMISSIONS) {
            eps = EXPOSED_PERMISSIONS_CACHE.getIfPresent(EXPOSED_PERMISSIONS);
            if (!CollectionUtils.isEmpty(eps)) {
                return eps;
            }
            eps = super.loadExposedPermissions();
            eps = Optional.ofNullable(eps).orElse(List.of());
            EXPOSED_PERMISSIONS_CACHE.put(EXPOSED_PERMISSIONS, eps);
            return eps;
        }
    }

    @Override
    public void refreshExposedPermissions() {
        EXPOSED_PERMISSIONS_CACHE.put(EXPOSED_PERMISSIONS, List.of());
        deleteCacheService.schedule(() -> EXPOSED_PERMISSIONS_CACHE.put(EXPOSED_PERMISSIONS, List.of()),
                5, TimeUnit.SECONDS);
    }

    @Override
    public void clear() {
        ALL_RESOURCE_PERMISSIONS_CACHE.invalidateAll();
        USER_PERMISSIONS_CACHE.invalidateAll();
        EXPOSED_PERMISSIONS_CACHE.invalidateAll();
        // 延迟双删
        deleteCacheService.schedule(() -> {
            ALL_RESOURCE_PERMISSIONS_CACHE.invalidateAll();
            USER_PERMISSIONS_CACHE.invalidateAll();
            EXPOSED_PERMISSIONS_CACHE.invalidateAll();
        }, 5, TimeUnit.SECONDS);
    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshAllResourcePermissions(PermissionEvent event) {
        super.listenerPermissionEventRefreshAllResourcePermissions(event);
        redisTemplate.convertAndSend(PERMISSION_EVENT_CHANNEL, String.valueOf(event.getPermissionId()));
    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshUserPermissions(PermissionEvent event) {
        super.listenerPermissionEventRefreshUserPermissions(event);
        redisTemplate.convertAndSend(PERMISSION_EVENT_CHANNEL, String.valueOf(event.getPermissionId()));
    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshExposedPermissions(PermissionEvent event) {
        super.listenerPermissionEventRefreshExposedPermissions(event);
        redisTemplate.convertAndSend(PERMISSION_EVENT_CHANNEL, String.valueOf(event.getPermissionId()));
    }

    @EventListener(value = RoleEvent.class)
    public void listenerRoleEventRefreshUserPermissions(RoleEvent event) {
        super.listenerRoleEventRefreshUserPermissions(event);
        redisTemplate.convertAndSend(ROLE_EVENT_CHANNEL, String.valueOf(event.getRoleId()));
    }

    @EventListener(value = RolePermissionEvent.class)
    public void listenerRolePermissionEventRefreshUserPermissions(RolePermissionEvent event) {
        super.listenerRolePermissionEventRefreshUserPermissions(event);
        redisTemplate.convertAndSend(ROLE_PERMISSION_EVENT_CHANNEL, String.valueOf(event.getRoleId()));
    }

    @EventListener(value = UserRoleEvent.class)
    public void listenerUserRoleEventRefreshUserPermissions(UserRoleEvent event) {
        super.listenerUserRoleEventRefreshUserPermissions(event);
        redisTemplate.convertAndSend(USER_ROLE_EVENT_CHANNEL, String.valueOf(event.getUserId()));
    }

    @EventListener(value = UserEvent.class)
    public void listenerUserEventRefreshUserPermissions(UserEvent event) {
        super.listenerUserEventRefreshUserPermissions(event);
        redisTemplate.convertAndSend(USER_EVENT_CHANNEL, String.valueOf(event.getUserId()));
    }

    @EventListener(value = ClearEvent.class)
    public void listenerClearEvent(ClearEvent event) {
        super.listenerClearEvent(event);
        redisTemplate.convertAndSend(CLEAR_EVENT_CHANNEL, CLEAR_EVENT_CHANNEL);
    }

    @Bean
    public MessageListener listenerPermissionEventRefreshAllResourcePermissions() {
        return (message, pattern) -> {
            Long permissionId = Long.getLong(new String(message.getBody()));
            if (permissionId == null) {
                return;
            }
            refreshAllResourcePermissions();
        };
    }

    @Bean
    public MessageListener listenerPermissionEventRefreshUserPermissions() {
        return (message, pattern) -> {
            Long permissionId = Long.getLong(new String(message.getBody()));
            if (permissionId == null) {
                return;
            }
            List<Long> roleIds = rolePermissionService.getRoleIds(permissionId);
            if (CollectionUtils.isEmpty(roleIds)) {
                return;
            }
            List<Long> userIds = userRoleService.getUserIds(roleIds);
            for (Long userId : userIds) {
                refreshUserPermissions(userId);
            }
        };
    }

    @Bean
    public MessageListener listenerPermissionEventRefreshExposedPermissions() {
        return (message, pattern) -> {
            Long permissionId = Long.getLong(new String(message.getBody()));
            if (permissionId == null) {
                return;
            }
            refreshExposedPermissions();
        };
    }

    @Bean
    public MessageListener listenerRoleEventRefreshUserPermissions() {
        return (message, pattern) -> {
            Long roleId = Long.getLong(new String(message.getBody()));
            if (roleId == null) {
                return;
            }
            List<Long> userIds = userRoleService.getUserIds(roleId);
            for (Long userId : userIds) {
                refreshUserPermissions(userId);
            }
        };
    }

    @Bean
    public MessageListener listenerRolePermissionEventRefreshUserPermissions() {
        return (message, pattern) -> {
            Long roleId = Long.getLong(new String(message.getBody()));
            if (roleId == null) {
                return;
            }
            List<Long> userIds = userRoleService.getUserIds(roleId);
            for (Long userId : userIds) {
                refreshUserPermissions(userId);
            }
        };
    }

    @Bean
    public MessageListener listenerUserRoleEventRefreshUserPermissions() {
        return (message, pattern) -> {
            Long userId = Long.getLong(new String(message.getBody()));
            if (userId == null) {
                return;
            }
            refreshUserPermissions(userId);
        };
    }

    @Bean
    public MessageListener listenerUserEventRefreshUserPermissions() {
        return (message, pattern) -> {
            Long userId = Long.getLong(new String(message.getBody()));
            if (userId == null) {
                return;
            }
            refreshUserPermissions(userId);
        };
    }

    @Bean
    public MessageListener listenerClearEvent() {
        return (message, pattern) -> {
            clear();
        };
    }


    @Bean
    public RedisMessageListenerContainer messageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listenerPermissionEventRefreshAllResourcePermissions(),
                new ChannelTopic(PERMISSION_EVENT_CHANNEL));
        container.addMessageListener(listenerPermissionEventRefreshUserPermissions(),
                new ChannelTopic(PERMISSION_EVENT_CHANNEL));
        container.addMessageListener(listenerPermissionEventRefreshExposedPermissions(),
                new ChannelTopic(PERMISSION_EVENT_CHANNEL));
        container.addMessageListener(listenerRoleEventRefreshUserPermissions(),
                new ChannelTopic(ROLE_EVENT_CHANNEL));
        container.addMessageListener(listenerRolePermissionEventRefreshUserPermissions(),
                new ChannelTopic(ROLE_PERMISSION_EVENT_CHANNEL));
        container.addMessageListener(listenerUserRoleEventRefreshUserPermissions(),
                new ChannelTopic(USER_ROLE_EVENT_CHANNEL));
        container.addMessageListener(listenerUserEventRefreshUserPermissions(),
                new ChannelTopic(USER_EVENT_CHANNEL));
        container.addMessageListener(listenerClearEvent(),
                new ChannelTopic(CLEAR_EVENT_CHANNEL));
        return container;
    }
}
