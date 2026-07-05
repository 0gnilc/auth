package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;

import java.util.List;

/**
 * 角色
 *
 * @author kyhns7
 */
public interface RoleService extends IService<RoleBo> {

    PageResult<RoleVo> getRolePage(RolePageDto sd);

    List<RoleVo> getRoles(RoleQueryDto qd);

    void createRole(RoleDto rd);

    RoleBo getRoleByCode(String code);

    void updateRole(RoleDto rd);

    void removeRole(Long id);

    List<RoleBo> getRoles(Long userId);
}

