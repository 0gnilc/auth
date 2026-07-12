package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;

import java.util.List;


/**
 * 权限
 *
 * @author kyhns7
 */
public interface PermissionService extends IService<PermissionBo> {

    void createPermission(PermissionDto dto);

    void updatePermission(PermissionDto dto);

    void removePermission(Long id);

    List<PermissionVo> getPermissions(PermissionQueryDto dto);

    PermissionBo getPermissionByCode(String code);

    List<PermissionBo> getPermissions(List<Long> ids);

    List<PermissionBo> getPermissions(Long userId);
}
