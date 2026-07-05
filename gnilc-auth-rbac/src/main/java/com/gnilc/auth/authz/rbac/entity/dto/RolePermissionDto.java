package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色关联权限(多对多)
 * 
 * @author kyhns7
 */
@Data
public class RolePermissionDto {
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
	private List<Long> permissionIds;
}
