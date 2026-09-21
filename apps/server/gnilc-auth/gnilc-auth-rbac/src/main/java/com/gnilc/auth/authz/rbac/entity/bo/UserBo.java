package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

/** RBAC 用户身份的持久化记录，与具体应用账号资料分离。 */
@Data
@TableName("az_user")
public class UserBo implements Serializable {
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
}
