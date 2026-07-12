package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.google.common.collect.Sets;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRoleBo> implements UserRoleService {

    private final ApplicationEventPublisher eventPublisher;

    public UserRoleServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void updateUserRole(UserRoleDto dto) {
        Preconditions.checkArgument(dto != null, "请填写用户角色信息");
        Long userId = dto.getUserId();
        List<Long> roleIds = dto.getRoleIds();
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

        List<UserRoleBo> bos = Sets.difference(newSet, oldSet)
                .stream()
                .map(roleId -> {
                    UserRoleBo bo = new UserRoleBo();
                    bo.setUserId(userId);
                    bo.setRoleId(roleId);
                    return bo;
                }).toList();
        if (!bos.isEmpty()) {
            saveBatch(bos);
        }

        eventPublisher.publishEvent(RbacAuthzEvent.of(
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
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public void bindRole(Long userId, Long roleId) {
        Preconditions.checkArgument(userId != null, "请选择用户");
        Preconditions.checkArgument(roleId != null, "请选择角色");
        UserRoleBo bo = getUserRole(userId, roleId);
        if (bo == null) {
            bo = new UserRoleBo();
            bo.setUserId(userId);
            bo.setRoleId(roleId);
            save(bo);
        }
        eventPublisher.publishEvent(RbacAuthzEvent.of(
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
        eventPublisher.publishEvent(RbacAuthzEvent.of(
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
                .distinct()
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
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }
}
