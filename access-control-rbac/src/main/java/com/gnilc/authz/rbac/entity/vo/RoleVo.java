package com.gnilc.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

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
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createTime;
	/**
	 * 角色标识
	 */
	private String symbol;
	/**
	 * 角色名
	 */
	private String name;
	/**
	 * 描述/备注
	 */
	private String remark;
	/**
	 * 系统内置,1是、0否
	 */
	private Boolean internal;
}
