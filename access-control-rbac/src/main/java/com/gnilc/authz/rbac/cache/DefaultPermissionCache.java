package com.gnilc.authz.rbac.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.bo.UserBo;
import com.gnilc.authz.rbac.service.PermissionService;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.gnilc.authz.rbac.service.event.*;
import com.gnilc.authz.rbac.service.event.*;
import com.gnilc.authz.rbac.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 默认实现,从数据库中加载数据,不做其他操作
 */
@Component
@ConditionalOnMissingBean(PermissionCache.class)
public class DefaultPermissionCache implements PermissionCache {

    @Autowired
    protected PermissionService permissionService;
    @Autowired
    protected RolePermissionService rolePermissionService;
    @Autowired
    protected UserRoleService userRoleService;
    @Autowired
    private UserServiceImpl userService;

    @Override
    public List<ResourcePermission> loadAllResourcePermissions() {
        return permissionService.list().stream().map(p ->
                new ResourcePermission(p.getSymbol(), p.getResource())
        ).toList();
    }

    @Override
    public void refreshAllResourcePermissions() {
        // no
    }

    @Override
    public List<Permission> loadUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        UserBo u = userService.getById(userId);
        if (u == null) {
            return List.of();
        }
        return permissionService.getPermissions(userId)
                .stream().map(p -> new Permission(p.getSymbol()))
                .toList();
    }

    @Override
    public void refreshUserPermissions(Long userId) {
        // no
    }

    @Override
    public List<Permission> loadExposedPermissions() {
        return permissionService.list(new LambdaQueryWrapper<PermissionBo>()
                        .eq(PermissionBo::getExposed, Boolean.TRUE))
                .stream()
                .map(p -> new Permission(p.getSymbol()))
                .toList();
    }

    @Override
    public void refreshExposedPermissions() {
        // no
    }

    @Override
    public void clear() {

    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshAllResourcePermissions(PermissionEvent event) {
        refreshAllResourcePermissions();
    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshUserPermissions(PermissionEvent event) {
        Long permissionId = event.getPermissionId();
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
    }

    @EventListener(value = PermissionEvent.class)
    public void listenerPermissionEventRefreshExposedPermissions(PermissionEvent event) {
        refreshExposedPermissions();
    }

    @EventListener(value = RoleEvent.class)
    public void listenerRoleEventRefreshUserPermissions(RoleEvent event) {
        Long roleId = event.getRoleId();
        if (roleId == null) {
            return;
        }
        List<Long> userIds = userRoleService.getUserIds(roleId);
        for (Long userId : userIds) {
            refreshUserPermissions(userId);
        }
    }

    @EventListener(value = RolePermissionEvent.class)
    public void listenerRolePermissionEventRefreshUserPermissions(RolePermissionEvent event) {
        Long roleId = event.getRoleId();
        if (roleId == null) {
            return;
        }
        List<Long> userIds = userRoleService.getUserIds(roleId);
        for (Long userId : userIds) {
            refreshUserPermissions(userId);
        }
    }

    @EventListener(value = UserRoleEvent.class)
    public void listenerUserRoleEventRefreshUserPermissions(UserRoleEvent event) {
        Long userId = event.getUserId();
        if (userId == null) {
            return;
        }
        refreshUserPermissions(userId);
    }

    @EventListener(value = UserEvent.class)
    public void listenerUserEventRefreshUserPermissions(UserEvent event) {
        Long userId = event.getUserId();
        if (userId == null) {
            return;
        }
        refreshUserPermissions(userId);
    }

    @EventListener(value = ClearEvent.class)
    public void listenerClearEvent(ClearEvent event) {
        clear();
    }
}
