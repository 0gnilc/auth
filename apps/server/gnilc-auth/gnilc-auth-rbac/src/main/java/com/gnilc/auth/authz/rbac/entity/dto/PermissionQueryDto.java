package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/** 权限列表筛选条件；未提供的条件不参与筛选。 */
@Data
public class PermissionQueryDto {
    /** 权限的稳定标识码；未提供时不按此条件筛选。 */
    private String code;
    /** 权限显示名称；未提供时不按此条件筛选。 */
    private String name;
    /** 受保护访问目标的标识；未提供时不按此条件筛选。 */
    private String targetIdentifier;
    /** 目标限定符，如 HTTP 方法；为空时不限定变体；未提供时不按此条件筛选。 */
    private String targetQualifier;
    /** 是否允许无角色授权的公开访问；不授予菜单可见性；未提供时不按此条件筛选。 */
    private Boolean publicAccess;
}
