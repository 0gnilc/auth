package com.gnilc.authz.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.authz.rbac.entity.bo.RoleMenuBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色关联权限(多对多)
 * 
 * @author kyhns7
 */
@Mapper
public interface RoleMenusDao extends BaseMapper<RoleMenuBo> {

}
