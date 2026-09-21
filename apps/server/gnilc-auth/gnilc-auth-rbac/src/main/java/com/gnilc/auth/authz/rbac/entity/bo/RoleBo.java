package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/** 角色定义的持久化记录，角色身份与所授予权限、菜单分别管理。 */
@Data
@TableName("az_role")
public class RoleBo implements Serializable {
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
    /** 角色的稳定标识码。 */
    private String code;
    /** 角色显示名称。 */
    private String name;
    /** 可选的管理备注。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /** 是否为系统维护的内置资源，决定适用的维护限制。 */
    private Boolean builtIn;
}
