package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色关联菜单(多对多)
 * 
 * @author kyhns7
 */
@Data
public class RoleMenuDto {
	/**
	 * id
	 */
	private Long id;
	/**
	 * 角色id
	 */
	private Long roleId;
	/**
	 * 菜单id
	 */
	private List<Long> menuIds;

}
