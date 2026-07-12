package com.gnilc.auth.authz.rbac.dao;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限
 * 
 * @author kyhns7
 */
@Mapper
public interface PermissionDao extends BaseMapper<PermissionBo> {
	
}
