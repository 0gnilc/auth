package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;

import java.util.List;

/**
 * 角色关联权限(多对多)
 *
 * @author kyhns7
 */
public interface RolePermissionService extends IService<RolePermissionBo> {

    List<Long> getPermissionIds(Long roleId);

    List<Long> getPermissionIds(List<Long> roleIds);

    List<Long> getRoleIds(Long permissionId);

    void updateRolePermission(RolePermissionDto dto);
}
