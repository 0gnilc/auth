package com.gnilc.auth.authz.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色关联权限(多对多)
 * 
 * @author kyhns7
 */
@Mapper
public interface RolePermissionDao extends BaseMapper<RolePermissionBo> {

}
