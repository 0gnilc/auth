package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;

import java.util.List;

/**
 * 用户
 *
 * @author kyhns7
 */
public interface UserService extends IService<UserBo> {

    /**
     * @return 用户id
     */
    Long createUser();

    /**
     *
     * @param userId 用户id
     */
    boolean removeUser(Long userId);

    /**
     * 绑定角色
     *
     * @param userId 用户id
     * @param roleCode 角色标识
     */
    boolean bindRole(Long userId, String roleCode);

    /**
     * 解绑角色
     * @param userId 用户id
     * @param roleCode 角色标识
     */
    boolean unbindRole(Long userId, String roleCode);

    /**
     * 根据用户id获取绑定的所有角色
     * @param userId 用户id
     * @return 角色列表
     */
    List<RoleBo> getRoles(Long userId);

    /**
     * 判断用户是否拥有指定角色
     * @param userId 用户id
     * @param roleCode 角色标识
     * @return 拥有返回true,否则返回false
     */
    boolean checkRole(Long userId, String roleCode);

    /**
     * 根据用户id获取用户的所有权限
     * @param userId 用户id
     * @return 角色列表
     */
    List<PermissionBo> getPermissions(Long userId);

    /**
     * 根据用户id获取用户的所有菜单(包含按钮)
     * @param userId 用户id
     * @return 菜单列表
     */
    List<MenuBo> getMenus(Long userId);
    /**
     * 根据用户id获取用户信息
     * @param userId 用户id
     * @return 用户信息
     */
    UserBo geUser(Long userId);
}

