package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/** 访问目标权限的持久化记录，公开访问属性与角色授予分别管理。 */
@Data
@TableName("az_permission")
public class PermissionBo implements Serializable {
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
    /** 权限的稳定标识码。 */
    private String code;
    /** 权限显示名称。 */
    private String name;
    /** 受保护访问目标的标识。 */
    private String targetIdentifier;
    /** 目标限定符，如 HTTP 方法；为空时不限定变体。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String targetQualifier;
    /** 可选的管理备注。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /** 是否允许无角色授权的公开访问；不授予菜单可见性。 */
    private Boolean publicAccess;
    /** 是否为系统维护的内置资源，决定适用的维护限制。 */
    private Boolean builtIn;
}
