package com.gnilc.auth.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;

/**
 * 权限
 *
 * @author kyhns7
 */
@Data
public class PermissionVo {
    /**
     * id
     */
    private Long id;
    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;
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
    private String targetQualifier;
    /**
     * 描述/备注
     */
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
