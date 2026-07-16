package com.gnilc.auth.authz.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("""
            WITH RECURSIVE menu_subtree (id) AS (
                SELECT id FROM az_menu WHERE id = #{rootId}
                UNION DISTINCT
                SELECT menu.id
                FROM az_menu menu
                JOIN menu_subtree subtree ON menu.pid = subtree.id
            )
            SELECT id FROM menu_subtree
            """)
    List<Long> selectSubtreeIdsIncludingDeleted(@Param("rootId") Long rootId);
}
