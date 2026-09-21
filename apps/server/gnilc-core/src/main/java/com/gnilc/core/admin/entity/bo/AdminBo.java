package com.gnilc.core.admin.entity.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/** 管理员身份与资料的持久化记录。 */
@Data
@TableName("sys_admin")
public class AdminBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除标记：0 表示有效，1 表示已删除。 */
    private Integer del;

    /** 记录创建时间，使用 UTC 时间点。 */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    /** 记录最近更新时间，使用 UTC 时间点；尚未更新时可为空。 */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;

    /** 对应的 RBAC 全局用户主键，与管理员资料主键不同。 */
    private Long userId;

    /** 管理员登录用户名。 */
    private String username;

    /** 管理员登录密码的 BCrypt 哈希。 */
    private String password;

    /** 展示昵称。 */
    private String nickname;

    /** 可选头像 URL；未设置时为空。 */
    @TableField(value = "avatar", updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    /** 可选的管理员说明。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 管理员登录后的默认首页路径。 */
    private String homePath;

    /** 账户启用状态：true 为启用，false 为停用。 */
    private Boolean status;
}
