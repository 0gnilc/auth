package com.gnilc.core.admin.entity.dto;

import lombok.Data;

import java.util.List;

/** 指定管理员的角色集合替换输入，基础管理员角色由服务器保证保留。 */
@Data
public class AdminRoleDto {
    /** 目标管理员主键。 */
    private Long id;

    /** 目标角色编码集合，null 或空列表移除可选角色绑定，服务器仍补齐 admin 基础角色。 */
    private List<String> roleCodes;
}
