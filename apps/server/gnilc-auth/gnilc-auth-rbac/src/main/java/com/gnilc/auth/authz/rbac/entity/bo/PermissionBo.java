package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/**
 * 权限
 *
 * @author kyhns7
 */
@Data
@TableName("az_permission")
public class PermissionBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 是否删除,0未删除、1已删除
     */
    private Integer del;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;
    /**
     * 权限标识
     */
    private String code;
    /**
     * 权限名称
     */
    private String name;
    /**
     * 访问目标标识
     */
    private String targetIdentifier;
    /**
     * 访问目标限定符
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String targetQualifier;
    /**
     * 描述/备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /**
     * 是否公开访问,0否、1是
     */
    private Boolean publicAccess;
    /**
     * 是否系统内置,0否、1是
     */
    private Boolean builtIn;
}
