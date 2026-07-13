package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/**
 * 权限
 *
 * @author kyhns7
 */
@Data
public class PermissionQueryDto {
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
     * 是否公开访问,0否、1是
     */
    private Boolean publicAccess;
}
