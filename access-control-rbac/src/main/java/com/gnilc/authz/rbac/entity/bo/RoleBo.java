package com.gnilc.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 角色
 *
 * @author kyhns7
 */
@Data
@TableName("ac_role")
public class RoleBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 是否删除
     */
    private Integer del;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 角色标识
     */
    private String symbol;
    /**
     * 角色名
     */
    private String name;
    /**
     * 描述/备注
     */
    private String remark;
    /**
     * 是否系统内置
     *
     */
    private Boolean internal;
}
