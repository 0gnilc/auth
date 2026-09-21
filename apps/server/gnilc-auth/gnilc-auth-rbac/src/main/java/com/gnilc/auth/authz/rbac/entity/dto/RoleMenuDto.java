package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/** 整体替换角色菜单授权的输入，保存时补齐所选菜单的祖先。 */
@Data
public class RoleMenuDto {
	/** 保留的绑定记录 ID 字段；当前整体授权操作以 roleId 为目标，不使用此值。 */
	private Long id;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;
	/** 目标菜单 ID 集合；空值或空集合清空授权，非空集合保存时补齐祖先菜单。 */
	private List<Long> menuIds;

}
