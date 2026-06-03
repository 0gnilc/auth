package com.gnilc.authz.rbac.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限
 * 
 * @author kyhns7
 */
@Data
public class PermissionVo {
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
	 * 权限标识
	 */
	private String symbol;
	/**
	 * 权限名称
	 */
	private String name;
	/**
	 * 描述/备注
	 */
	private String remark;
	/**
	 * 权限主体对象
	 */
	private String resource;
	/**
	 * 是否公开
	 */
	private Boolean exposed;
}
