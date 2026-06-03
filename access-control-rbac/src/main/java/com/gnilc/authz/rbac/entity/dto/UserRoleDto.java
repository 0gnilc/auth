package com.gnilc.authz.rbac.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户关联角色(多对多)
 *
 * @author kyhns7
 */
@Data
public class UserRoleDto {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 角色id
     */
    private List<Long> roleIds;

}
