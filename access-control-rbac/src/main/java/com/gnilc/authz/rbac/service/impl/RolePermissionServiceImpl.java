package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.RolePermissionDao;
import com.gnilc.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.gnilc.authz.rbac.service.event.RolePermissionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


@Service("rolePermissionService")
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionDao, RolePermissionBo> implements RolePermissionService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        Preconditions.checkArgument(roleId != null, "roleId == null");
        List<RolePermissionBo> list = list(new LambdaQueryWrapper<RolePermissionBo>()
                .eq(RolePermissionBo::getRoleId, roleId));
        return list.stream().map(RolePermissionBo::getPermissionId).toList();
    }

    @Override
    public List<Long> getPermissionIds(List<Long> roleIds) {
        Preconditions.checkArgument(roleIds != null, "roleIds == null");
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return list(new LambdaQueryWrapper<RolePermissionBo>()
                .in(RolePermissionBo::getRoleId, roleIds))
                .stream()
                .map(RolePermissionBo::getPermissionId).toList();
    }

    @Transactional
    @Override
    public void saveRolePermission(RolePermissionDto rolePermissionDto) {
        Long roleId = rolePermissionDto.getRoleId();
        List<Long> permissionIds = rolePermissionDto.getPermissionIds();
        Preconditions.checkArgument(roleId != null, "请刷新后再说");
        remove(new LambdaQueryWrapper<RolePermissionBo>()
                .eq(RolePermissionBo::getRoleId, roleId));
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        List<RolePermissionBo> rps = permissionIds.stream().map(permissionId -> {
            RolePermissionBo rolePermission = new RolePermissionBo();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            return rolePermission;
        }).toList();
        saveBatch(rps);

        publisher.publishEvent(new RolePermissionEvent(this, roleId));
    }

    @Override
    public List<Long> getRoleIds(Long permissionId) {
        if (permissionId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<RolePermissionBo>()
                .select(RolePermissionBo::getRoleId)
                .eq(RolePermissionBo::getPermissionId, permissionId))
                .stream().map(RolePermissionBo::getRoleId).toList();
    }
}