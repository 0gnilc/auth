package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** 一条 RBAC 用户与角色绑定的持久化记录。 */
@Data
@TableName("az_user_role")
public class UserRoleBo implements Serializable {
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
	/** 所关联 RBAC 用户的数据库 ID。 */
	private Long userId;
	/** 所关联角色的数据库 ID。 */
	private Long roleId;

}
