package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.RolePermissionDao;
import com.gnilc.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("rolePermissionService")
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionDao, RolePermissionBo> implements RolePermissionService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        Preconditions.checkArgument(roleId != null, "请选择角色");
        return lambdaQuery()
                .select(RolePermissionBo::getPermissionId)
                .eq(RolePermissionBo::getRoleId, roleId)
                .list()
                .stream().map(RolePermissionBo::getPermissionId)
                .toList();
    }

    @Override
    public List<Long> getPermissionIds(List<Long> roleIds) {
        Preconditions.checkArgument(roleIds != null, "请选择角色");
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return lambdaQuery()
                .select(RolePermissionBo::getPermissionId)
                .in(RolePermissionBo::getRoleId, roleIds)
                .list()
                .stream()
                .map(RolePermissionBo::getPermissionId)
                .toList();
    }

    @Transactional
    @Override
    public void updateRolePermission(RolePermissionDto rpd) {
        Preconditions.checkArgument(rpd != null, "请填写角色权限信息");
        Long roleId = rpd.getRoleId();
        List<Long> permissionIds = rpd.getPermissionIds();
        Preconditions.checkArgument(roleId != null, "请选择角色");

        Set<Long> oldSet = lambdaQuery()
                .select(RolePermissionBo::getPermissionId)
                .eq(RolePermissionBo::getRoleId, roleId)
                .list()
                .stream()
                .map(RolePermissionBo::getPermissionId)
                .collect(Collectors.toSet());

        Set<Long> newSet = CollectionUtils.isEmpty(permissionIds) ? Set.of() : Sets.newHashSet(permissionIds);
        Set<Long> removeSet = Sets.difference(oldSet, newSet);
        if (!removeSet.isEmpty()) {
            lambdaUpdate()
                    .eq(RolePermissionBo::getRoleId, roleId)
                    .in(RolePermissionBo::getPermissionId, removeSet)
                    .remove();
        }

        List<RolePermissionBo> rpbs = Sets.difference(newSet, oldSet)
                .stream()
                .map(permissionId -> {
                    RolePermissionBo rpb = new RolePermissionBo();
                    rpb.setRoleId(roleId);
                    rpb.setPermissionId(permissionId);
                    return rpb;
                }).toList();
        if (!rpbs.isEmpty()) {
            saveBatch(rpbs);
        }
        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE_PERMISSION,
                RbacAuthzEvent.Action.REPLACE,
                roleId));
    }

    @Override
    public List<Long> getRoleIds(Long permissionId) {
        if (permissionId == null) {
            return List.of();
        }
        return lambdaQuery()
                .select(RolePermissionBo::getRoleId)
                .eq(RolePermissionBo::getPermissionId, permissionId)
                .list()
                .stream()
                .map(RolePermissionBo::getRoleId)
                .toList();
    }
}
