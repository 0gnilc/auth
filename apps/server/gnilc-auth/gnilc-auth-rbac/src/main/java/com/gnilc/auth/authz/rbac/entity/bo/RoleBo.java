package com.gnilc.auth.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/**
 * 角色
 *
 * @author kyhns7
 */
@Data
@TableName("az_role")
public class RoleBo implements Serializable {
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
     * 角色标识
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 描述/备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /**
     * 是否系统内置,0否、1是
     */
    private Boolean builtIn;
}
