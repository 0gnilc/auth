package com.gnilc.auth.authz.rbac.entity.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 一条角色菜单绑定的查询结果。 */
@Data
public class RoleMenuVo  {

	/** 记录的数据库主键。 */
	private Long id;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;
	/** 所关联菜单的数据库 ID。 */
	private Long menuId;

}
