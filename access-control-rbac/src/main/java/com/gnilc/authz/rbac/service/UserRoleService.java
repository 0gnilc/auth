package com.gnilc.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.authz.rbac.entity.dto.UserRoleDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户关联角色(多对多)
 *
 * @author kyhns7
 */
@Service
public interface UserRoleService extends IService<UserRoleBo> {

    void updateUserRole(UserRoleDto urd);

    List<Long> getRoleIds(Long userId);

    void bindRole(Long userId, Long roleId);

    void unbindRole(Long userId, Long roleId);

    List<Long> getUserIds(List<Long> roleIds);

    List<Long> getUserIds(Long roleId);

    UserRoleBo getUserRole(Long userId, Long roleId);
}

