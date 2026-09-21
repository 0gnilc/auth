package com.gnilc.auth.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;

/** 权限管理查询结果。 */
@Data
public class PermissionVo {
    /** 记录的数据库主键。 */
    private Long id;
    /** 记录创建的 UTC 时间点。 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;
    /** 权限的稳定标识码。 */
    private String code;
    /** 权限显示名称。 */
    private String name;
    /** 受保护访问目标的标识。 */
    private String targetIdentifier;
    /** 目标限定符，如 HTTP 方法；为空时不限定变体。 */
    private String targetQualifier;
    /** 可选的管理备注。 */
    private String remark;
    /** 是否允许无角色授权的公开访问；不授予菜单可见性。 */
    private Boolean publicAccess;
    /** 是否为系统维护的内置资源，决定适用的维护限制。 */
    private Boolean builtIn;
}
