package com.gnilc.authz.rbac.dao;

import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色
 * 
 * @author kyhns7
 */
@Mapper
public interface RoleDao extends BaseMapper<RoleBo> {
	
}
