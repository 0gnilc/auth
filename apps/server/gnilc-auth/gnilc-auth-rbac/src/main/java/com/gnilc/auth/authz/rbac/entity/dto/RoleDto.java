package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/** 创建或更新角色定义的输入，更新时指定角色 ID。 */
@Data
public class RoleDto {
    /** 更新目标的数据库 ID；创建操作不使用该值。 */
    private Long id;
    /** 角色的稳定标识码。 */
    private String code;
    /** 角色显示名称。 */
    private String name;
    /** 可选的管理备注。 */
    private String remark;
}
