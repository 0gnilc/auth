package com.gnilc.auth.authz.rbac.entity.dto;

import lombok.Data;

/**
 * 角色
 *
 * @author kyhns7
 */
@Data
public class RoleQueryDto {
    /**
     * 角色标识
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 是否系统内置,0否、1是
     */
    private Boolean builtIn;
}
