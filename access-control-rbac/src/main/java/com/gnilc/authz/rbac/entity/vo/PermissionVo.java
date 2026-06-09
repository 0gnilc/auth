package com.gnilc.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
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
}
