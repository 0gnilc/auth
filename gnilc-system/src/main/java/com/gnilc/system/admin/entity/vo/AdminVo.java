package com.gnilc.system.admin.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理员资料响应。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AdminVo {
    /**
     * 管理员 ID。
     */
    private Long id;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * RBAC 全局用户 ID。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 头像地址。
     */
    private String avatar;

    /**
     * 管理员描述。
     */
    private String desc;

    /**
     * 默认首页路径。
     */
    private String homePath;

    /**
     * 启用状态。
     */
    private Boolean status;

    /**
     * 已绑定角色标识。
     */
    private List<String> roleCodes;
}
