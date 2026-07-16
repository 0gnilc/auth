package com.gnilc.auth.authz.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单
 * 
 * @author kyhns7
 */
@Mapper
public interface MenuDao extends BaseMapper<MenuBo> {

    /**
     * 查询包含逻辑删除节点在内的完整菜单子树。
     */
    List<Long> selectSubtreeIdsWithDeleted(@Param("rootId") Long rootId);
}
