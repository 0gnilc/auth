package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.dao.UserDao;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.entity.bo.UserBo;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Override
    public Long createUser() {
        UserBo ub = new UserBo();
        save(ub);
        return ub.getId();
    }

    @Transactional
    @Override
    public boolean removeUser(Long userId) {
        removeById(userId);
        publisher.publishEvent(RbacAuthzEvent.of(
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
        RoleBo rb = roleService.getRoleByCode(roleCode);
        if (rb == null) {
            return false;
        }
        userRoleService.bindRole(userId, rb.getId());
        return true;
    }

    @Transactional
    @Override
    public boolean unbindRole(Long userId, String roleCode) {
        if (userId == null || StringUtils.isBlank(roleCode)) {
            return false;
        }
        RoleBo rb = roleService.getRoleByCode(roleCode);
        if (rb == null) {
            return false;
        }
        userRoleService.unbindRole(userId, rb.getId());
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
        RoleBo rb = roleService.getRoleByCode(roleCode);
        if (rb == null) {
            return false;
        }
        return userRoleService.getUserRole(userId, rb.getId()) != null;
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
    public UserBo geUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return getById(userId);
    }
}
