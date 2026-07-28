package com.gnilc.auth.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;

/**
 * 角色
 *
 * @author kyhns7
 */
@Data
public class RoleVo  {
    /**
     * id
     */
    private Long id;
    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;
    /**
     * 角色标识
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 描述/备注
     */
    private String remark;
    /**
     * 是否系统内置,0否、1是
     */
    private Boolean builtIn;
}
