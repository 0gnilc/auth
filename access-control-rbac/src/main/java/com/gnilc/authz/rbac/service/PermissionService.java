package com.gnilc.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.authz.rbac.entity.vo.PermissionVo;

import java.util.List;


/**
 * 权限
 *
 * @author kyhns7
 */
public interface PermissionService extends IService<PermissionBo> {

    void savePermission(PermissionDto pd);

    void modifyPermission(PermissionDto pd);

    void removePermission(Long id);

    List<PermissionVo> getPermissions(PermissionQueryDto qd);

    PermissionBo getPermissionBySymbol(String symbol);

    List<PermissionBo> getPermissions(List<Long> ids);

    List<PermissionBo> getPermissions(Long userId);
}

