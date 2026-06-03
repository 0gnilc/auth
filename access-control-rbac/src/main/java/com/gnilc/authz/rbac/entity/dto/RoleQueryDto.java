package com.gnilc.authz.rbac.entity.dto;

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
    private String symbol;
    /**
     * 角色名
     */
    private String name;
    /**
     * 系统内置,1是、0否
     */
    private Boolean internal;
}
