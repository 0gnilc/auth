package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.dao.UserDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserBo> implements UserService {
    private final ApplicationEventPublisher eventPublisher;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final MenuService menuService;

    public UserServiceImpl(ApplicationEventPublisher eventPublisher,
                           RoleService roleService,
                           UserRoleService userRoleService,
                           PermissionService permissionService,
                           MenuService menuService) {
        this.eventPublisher = eventPublisher;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
        this.permissionService = permissionService;
        this.menuService = menuService;
    }

    @Transactional
    @Override
    public Long createUser() {
        UserBo bo = new UserBo();
        save(bo);
        return bo.getId();
    }

    @Transactional
    @Override
    public boolean removeUser(Long userId) {
        removeById(userId);
        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER,
                RbacAuthzEvent.Action.DELETE,
                userId));
        return true;
    }

    @Transactional
    @Override
    public boolean bindRole(Long userId, String roleCode) {
        if (userId == null || StringUtils.isBlank(roleCode)) {
            return false;
        }
        RoleBo bo = roleService.getRoleByCode(roleCode);
        if (bo == null) {
            return false;
        }
        userRoleService.bindRole(userId, bo.getId());
        return true;
    }

    @Transactional
    @Override
    public boolean unbindRole(Long userId, String roleCode) {
        if (userId == null || StringUtils.isBlank(roleCode)) {
            return false;
        }
        RoleBo bo = roleService.getRoleByCode(roleCode);
        if (bo == null) {
            return false;
        }
        userRoleService.unbindRole(userId, bo.getId());
        return true;
    }

    @Override
    public List<RoleBo> getRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return roleService.getRoles(userId);
    }

    @Override
    public boolean checkRole(Long userId, String roleCode) {
        RoleBo bo = roleService.getRoleByCode(roleCode);
        if (bo == null) {
            return false;
        }
        return userRoleService.getUserRole(userId, bo.getId()) != null;
    }

    @Override
    public List<PermissionBo> getPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return permissionService.getPermissions(userId);
    }

    @Override
    public List<MenuBo> getMenus(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return menuService.getMenus(userId);
    }

    @Override
    public UserBo getUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return getById(userId);
    }
}
