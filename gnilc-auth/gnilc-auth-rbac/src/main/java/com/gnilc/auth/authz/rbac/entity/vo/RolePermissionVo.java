package com.gnilc.auth.authz.rbac.entity.vo;

import lombok.Data;

/**
 * 角色关联权限(多对多)
 * 
 * @author kyhns7
 */
@Data
public class RolePermissionVo {
	/**
	 * id
	 */
	private Long id;
	/**
	 * 角色id
	 */
	private Long roleId;
	/**
	 * 权限id
	 */
	private Long permissionId;
}
