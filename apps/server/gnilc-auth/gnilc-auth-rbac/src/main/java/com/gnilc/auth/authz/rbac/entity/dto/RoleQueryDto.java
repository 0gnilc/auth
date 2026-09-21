package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/** 角色列表筛选条件；未提供的条件不参与筛选。 */
@Data
public class RoleQueryDto {
    /** 角色的稳定标识码；未提供时不按此条件筛选。 */
    private String code;
    /** 角色显示名称；未提供时不按此条件筛选。 */
    private String name;
    /** 是否为系统维护的内置资源，决定适用的维护限制；未提供时不按此条件筛选。 */
    private Boolean builtIn;
}
