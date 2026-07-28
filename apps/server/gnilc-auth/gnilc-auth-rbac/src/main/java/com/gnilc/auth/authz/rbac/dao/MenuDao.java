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
     * 查询完整菜单子树。
     *
     * @param deleted 是否包含逻辑删除节点
     */
    List<Long> getSubtreeIds(@Param("rootId") Long rootId, @Param("deleted") boolean deleted);
}
