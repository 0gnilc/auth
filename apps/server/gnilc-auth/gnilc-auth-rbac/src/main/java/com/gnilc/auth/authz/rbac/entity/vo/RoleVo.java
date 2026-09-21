package com.gnilc.auth.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;

/** 角色管理查询结果。 */
@Data
public class RoleVo  {
    /** 记录的数据库主键。 */
    private Long id;
    /** 记录创建的 UTC 时间点。 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;
    /** 角色的稳定标识码。 */
    private String code;
    /** 角色显示名称。 */
    private String name;
    /** 可选的管理备注。 */
    private String remark;
    /** 是否为系统维护的内置资源，决定适用的维护限制。 */
    private Boolean builtIn;
}
