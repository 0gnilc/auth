package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.auth.authz.rbac.dao.RolePermissionDao;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.google.common.collect.Sets;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("rolePermissionService")
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionDao, RolePermissionBo>
        implements RolePermissionService {

    private final ApplicationEventPublisher eventPublisher;
    private final I18nMessageService messages;

    public RolePermissionServiceImpl(ApplicationEventPublisher eventPublisher, I18nMessageService messages) {
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        Preconditions.checkArgument(roleId != null, messages.get("rbac.role.selection.required"));
        return lambdaQuery()
                .select(RolePermissionBo::getPermissionId)
                .eq(RolePermissionBo::getRoleId, roleId)
                .list()
                .stream()
                .map(RolePermissionBo::getPermissionId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getPermissionIds(List<Long> roleIds) {
        Preconditions.checkArgument(roleIds != null, "At least one role must be selected.");
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return lambdaQuery()
                .select(RolePermissionBo::getPermissionId)
                .in(RolePermissionBo::getRoleId, roleIds)
                .list()
                .stream()
                .map(RolePermissionBo::getPermissionId)
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public void updateRolePermission(RolePermissionDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.assignment.rolePermission.required"));
        Long roleId = dto.getRoleId();
        List<Long> permissionIds = dto.getPermissionIds();
        Preconditions.checkArgument(roleId != null, messages.get("rbac.role.selection.required"));

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

        List<RolePermissionBo> bos = Sets.difference(newSet, oldSet)
                .stream()
                .map(permissionId -> {
                    RolePermissionBo bo = new RolePermissionBo();
                    bo.setRoleId(roleId);
                    bo.setPermissionId(permissionId);
                    return bo;
                }).toList();
        if (!bos.isEmpty()) {
            saveBatch(bos);
        }
        eventPublisher.publishEvent(RbacAuthzEvent.of(
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
                .distinct()
                .toList();
    }
}
