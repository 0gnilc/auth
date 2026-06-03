package com.gnilc.authz.rbac.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色
 * 
 * @author kyhns7
 */
@Data
public class RoleDto {
	/**
	 * id
	 */
	private Long id;
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
}
