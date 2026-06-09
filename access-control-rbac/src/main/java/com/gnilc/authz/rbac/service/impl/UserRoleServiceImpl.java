package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.UserRoleDao;
import com.gnilc.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRoleBo> implements UserRoleService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Transactional
    @Override
    public void updateUserRole(UserRoleDto urd) {
        Preconditions.checkArgument(urd != null, "请填写用户角色信息");
        Long userId = urd.getUserId();
        List<Long> roleIds = urd.getRoleIds();
        Preconditions.checkArgument(userId != null, "请选择用户");
        Set<Long> oldSet = lambdaQuery()
                .select(UserRoleBo::getRoleId)
                .eq(UserRoleBo::getUserId, userId)
                .list()
                .stream()
                .map(UserRoleBo::getRoleId)
                .collect(Collectors.toSet());
        Set<Long> newSet = CollectionUtils.isEmpty(roleIds) ? Set.of() : Sets.newHashSet(roleIds);

        Set<Long> removeSet = Sets.difference(oldSet, newSet);
        if (!removeSet.isEmpty()) {
            lambdaUpdate()
                    .eq(UserRoleBo::getUserId, userId)
                    .in(UserRoleBo::getRoleId, removeSet)
                    .remove();
        }

        List<UserRoleBo> urbs = Sets.difference(newSet, oldSet)
                .stream()
                .map(roleId -> {
                    UserRoleBo urb = new UserRoleBo();
                    urb.setUserId(userId);
                    urb.setRoleId(roleId);
                    return urb;
                }).toList();
        if (!urbs.isEmpty()) {
            saveBatch(urbs);
        }

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                userId));
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return lambdaQuery()
                .select(UserRoleBo::getRoleId)
                .eq(UserRoleBo::getUserId, userId)
                .list()
                .stream()
                .map(UserRoleBo::getRoleId)
                .toList();
    }

    @Transactional
    @Override
    public void bindRole(Long userId, Long roleId) {
        Preconditions.checkArgument(userId != null, "请选择用户");
        Preconditions.checkArgument(roleId != null, "请选择角色");
        UserRoleBo urb = new UserRoleBo();
        urb.setUserId(userId);
        urb.setRoleId(roleId);
        saveOrUpdate(urb);
        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                userId));
    }

    @Transactional
    @Override
    public void unbindRole(Long userId, Long roleId) {
        Preconditions.checkArgument(userId != null, "请选择用户");
        Preconditions.checkArgument(roleId != null, "请选择角色");
        remove(new LambdaQueryWrapper<UserRoleBo>()
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId));
        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                userId));
    }

    @Override
    public List<Long> getUserIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return lambdaQuery()
                .select(UserRoleBo::getUserId)
                .in(UserRoleBo::getRoleId, roleIds)
                .list()
                .stream()
                .map(UserRoleBo::getUserId)
                .toList();
    }

    @Override
    public List<Long> getUserIds(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return getUserIds(List.of(roleId));
    }

    @Override
    public UserRoleBo getUserRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId)
                .one();
    }
}
