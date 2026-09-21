package com.gnilc.auth.authz.rbac.entity.vo;

import lombok.Data;

/** 一条角色权限绑定的查询结果。 */
@Data
public class RolePermissionVo {
	/** 记录的数据库主键。 */
	private Long id;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;
	/** 所关联权限的数据库 ID。 */
	private Long permissionId;
}
