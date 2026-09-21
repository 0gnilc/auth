package com.gnilc.auth.authz.rbac.entity.dto;


import com.gnilc.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色分页查询条件，分页规则继承自 {@link PageParams}。 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RolePageDto extends PageParams {
    /** 角色的稳定标识码；未提供时不按此条件筛选。 */
    private String code;
    /** 角色显示名称；未提供时不按此条件筛选。 */
    private String name;
}
