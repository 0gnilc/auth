package com.gnilc.core.admin.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 管理员资料及已绑定角色的输出。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AdminVo {
    /** 记录主键。 */
    private Long id;

    /** 记录创建时间，使用 UTC 时间点。 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;

    /** 对应的 RBAC 全局用户主键，与管理员资料主键不同。 */
    private Long userId;

    /** 管理员登录用户名。 */
    private String username;

    /** 展示昵称。 */
    private String nickname;

    /** 可选头像 URL；未设置时为空。 */
    private String avatar;


    /** 可选的管理员说明。 */
    private String desc;

    /** 管理员登录后的默认首页路径。 */
    private String homePath;

    /** 账户启用状态：true 为启用，false 为停用。 */
    private Boolean status;

    /** 已绑定的角色编码集合，包含管理员基础访问角色。 */
    private List<String> roleCodes;
}
