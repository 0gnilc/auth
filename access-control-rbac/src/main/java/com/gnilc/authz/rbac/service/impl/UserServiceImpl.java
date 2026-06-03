package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.dao.UserDao;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.entity.bo.UserBo;
import com.gnilc.authz.rbac.service.*;
import com.gnilc.authz.rbac.service.*;
import com.gnilc.authz.rbac.service.event.CrudEvent;
import com.gnilc.authz.rbac.service.event.UserEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserBo> implements UserService {
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private MenuService menuService;

    @Override
    public Long createUser() {
        UserBo user = new UserBo();
        save(user);
        return user.getId();
    }

    @Override
    public boolean removeUser(Long userId) {
        removeById(userId);
        publisher.publishEvent(new UserEvent(this, CrudEvent.Event.DELETE, userId));
        return true;
    }

    @Override
    public boolean bindRole(Long userId, String roleSymbol) {
        if (userId == null || StringUtils.isBlank(roleSymbol)) {
            return false;
        }
        RoleBo role = roleService.getRoleBySymbol(roleSymbol);
        if (role == null) {
            return false;
        }
        userRoleService.bindRole(userId, role.getId());
        return true;
    }

    @Override
    public boolean unbindRole(Long userId, String roleSymbol) {
        if (userId == null || StringUtils.isBlank(roleSymbol)) {
            return false;
        }
        RoleBo role = roleService.getRoleBySymbol(roleSymbol);
        if (role == null) {
            return false;
        }
        userRoleService.unbindRole(userId, role.getId());
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
    public boolean checkRole(Long userId, String roleSymbol) {
        RoleBo role = roleService.getRoleBySymbol(roleSymbol);
        if (role == null) {
            return false;
        }
        return userRoleService.getUserRole(userId, role.getId()) != null;
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
}