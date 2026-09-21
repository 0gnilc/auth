package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/** 创建或更新访问目标权限的输入，更新时指定权限 ID。 */
@Data
public class PermissionDto {
    /** 更新目标的数据库 ID；创建操作不使用该值。 */
    private Long id;
    /** 权限的稳定标识码。 */
    private String code;
    /** 权限显示名称。 */
    private String name;
    /** 受保护访问目标的标识。 */
    private String targetIdentifier;
    /** 目标限定符，如 HTTP 方法；为空时不限定变体。 */
    private String targetQualifier;
    /** 可选的管理备注。 */
    private String remark;
    /** 是否允许无角色授权的公开访问；不授予菜单可见性。 */
    private Boolean publicAccess;
}
