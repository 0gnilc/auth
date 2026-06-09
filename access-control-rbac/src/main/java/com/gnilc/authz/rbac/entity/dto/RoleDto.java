package com.gnilc.authz.rbac.entity.dto;

import lombok.Data;

/**
 * 角色
 *
 * @author kyhns7
 */
@Data
public class RoleDto {
    /**
     * id
     */
    private Long id;
    /**
     * 角色标识
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 描述/备注
     */
    private String remark;
}
