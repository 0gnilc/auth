package com.gnilc.authz.rbac.entity.dto;

import lombok.Data;

/**
 * 权限
 *
 * @author kyhns7
 */
@Data
public class PermissionQueryDto {
    /**
     * 权限标识
     */
    private String symbol;
    /**
     * 权限名称
     */
    private String name;
    /**
     * 权限主体对象
     */
    private String resource;
}
