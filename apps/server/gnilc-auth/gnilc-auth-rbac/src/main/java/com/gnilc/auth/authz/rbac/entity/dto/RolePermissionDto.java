package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/** 整体替换角色权限授权的输入。 */
@Data
public class RolePermissionDto {
	/** 保留的绑定记录 ID 字段；当前整体授权操作以 roleId 为目标，不使用此值。 */
	private Long id;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;
	/** 目标权限 ID 集合；空值或空集合清空授权，集合元素必须引用已有权限。 */
	private List<Long> permissionIds;
}
