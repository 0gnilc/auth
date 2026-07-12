package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;

import java.util.List;

/**
 * 用户关联角色(多对多)
 *
 * @author kyhns7
 */
public interface UserRoleService extends IService<UserRoleBo> {

    void updateUserRole(UserRoleDto dto);

    List<Long> getRoleIds(Long userId);

    void bindRole(Long userId, Long roleId);

    void unbindRole(Long userId, Long roleId);

    List<Long> getUserIds(List<Long> roleIds);

    List<Long> getUserIds(Long roleId);

    UserRoleBo getUserRole(Long userId, Long roleId);
}
