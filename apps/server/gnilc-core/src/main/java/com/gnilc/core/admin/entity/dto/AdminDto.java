package com.gnilc.core.admin.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;

/** 管理员创建、管理更新及当前资料更新共用输入，各操作按职责读取字段。 */
@Data
public class AdminDto {
    /** 管理更新时目标管理员主键；创建和当前资料更新不使用该字段。 */
    private Long id;

    /** 管理员登录用户名；创建必填，管理更新为 null 时保留原值。 */
    private String username;

    /** 明文登录密码；创建必填，管理更新为 null 时保留原值，非 null 必须满足 8 至 32 个字符、无空白并包含大小写、数字及特殊字符。 */
    private String password;

    /** 管理员昵称；创建及当前资料更新必填，管理更新为 null 时保留原值。 */
    private String nickname;

    /** 可选头像 URL；管理更新中省略则保留、显式 null 则清除，当前资料更新中 null 清除。 */
    private String avatar;

    /** JSON 是否显式提交头像字段，用于区分省略与清空，仅服务器内部使用。 */
    @JsonIgnore
    private boolean avatarSpecified;

    /** 可选管理员说明；管理更新中省略则保留、显式 null 则清除，当前资料更新中 null 清除。 */
    private String desc;

    /** JSON 是否显式提交说明字段，用于区分省略与清空，仅服务器内部使用。 */
    @JsonIgnore
    private boolean descSpecified;

    /** 默认首页路径；创建时 null 使用系统默认路径，管理更新时 null 保留原值。 */
    private String homePath;

    /** 目标启用状态；管理更新时 null 保留原值，停用会撤销管理员会话。 */
    private Boolean status;

    /** 目标角色编码；管理更新中 null 保留、空列表移除可选绑定，服务器始终补齐 admin 基础角色。 */
    private List<String> roleCodes;

    @JsonSetter
    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.avatarSpecified = true;
    }

    @JsonSetter
    public void setDesc(String desc) {
        this.desc = desc;
        this.descSpecified = true;
    }
}
