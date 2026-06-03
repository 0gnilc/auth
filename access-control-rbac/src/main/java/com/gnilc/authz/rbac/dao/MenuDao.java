package com.gnilc.authz.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单
 * 
 * @author kyhns7
 */
@Mapper
public interface MenuDao extends BaseMapper<MenuBo> {
    
}
