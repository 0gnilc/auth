package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/** 一条角色与菜单绑定的持久化记录。 */
@Data
@TableName("az_role_menu")
public class RoleMenuBo implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	/** 记录的数据库主键。 */
	@TableId(type = IdType.AUTO)
	private Long id;
	/** 逻辑删除标记：0 表示有效，1 表示已删除。 */
	private Integer del;
	/** 记录创建的 UTC 时间点。 */
	@TableField(fill = FieldFill.INSERT)
	private Instant createTime;
	/** 记录最近更新的 UTC 时间点；尚未记录更新时可为空。 */
	@TableField(fill = FieldFill.UPDATE)
	private Instant updateTime;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;
	/** 所关联菜单的数据库 ID。 */
	private Long menuId;

}
