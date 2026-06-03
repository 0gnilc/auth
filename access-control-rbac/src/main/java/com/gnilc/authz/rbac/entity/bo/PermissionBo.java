package com.gnilc.authz.rbac.entity.bo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 权限
 *
 * @author kyhns7
 */
@Data
@TableName("ac_permission")
public class PermissionBo implements Serializable {
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
     * 权限标识
     */
    private String symbol;
    /**
     * 权限名称
     */
    private String name;
    /**
     * 描述/备注
     */
    private String remark;
    /**
     * 受保护的资源
     */
    private String resource;
    /**
     * 是否公开
     */
    private Boolean exposed;
}
