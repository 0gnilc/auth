package com.gnilc.authz.rbac.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.authz.rbac.entity.bo.UserRoleBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关联角色(多对多)
 * 
 * @author kyhns7
 */
@Mapper
public interface UserRoleDao extends BaseMapper<UserRoleBo> {

}
