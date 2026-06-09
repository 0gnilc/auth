package com.gnilc.authz.rbac.entity.dto;


import com.gnilc.authz.rbac.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色
 *
 * @author kyhns7
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RolePageDto extends PageParams {
    /**
     * 角色标识
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;
}
