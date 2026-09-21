package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/** 整体替换用户角色绑定的输入，必须保留应用要求的基线角色。 */
@Data
public class UserRoleDto {
    /** 所关联 RBAC 用户的数据库 ID。 */
    private Long userId;
    /** 替换后的角色 ID 集合；空值或空集合表示无绑定，但移除必需基线角色会被拒绝。 */
    private List<Long> roleIds;

}
