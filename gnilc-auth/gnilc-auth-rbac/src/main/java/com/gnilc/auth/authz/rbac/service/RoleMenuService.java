package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;

import java.util.List;

/**
 * 角色关联菜单(多对多)
 *
 * @author kyhns7
 */
public interface RoleMenuService extends IService<RoleMenuBo> {

    List<Long> getMenuIds(Long roleId);

    List<Long> getMenuIds(List<Long> roleIds);

    void updateRoleMenu(RoleMenuDto dto);
}
