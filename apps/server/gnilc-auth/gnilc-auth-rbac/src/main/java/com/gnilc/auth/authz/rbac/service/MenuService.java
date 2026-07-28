package com.gnilc.auth.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;

import java.util.List;
import java.util.Set;

public interface MenuService extends IService<MenuBo> {

    List<MenuVo> getMenuTree();

    void createMenu(MenuDto dto);

    /**
     * 完整更新菜单。调用方必须提交完整菜单数据，省略字段不表示保留原值。
     */
    void updateMenu(MenuDto dto);

    MenuBo getMenuByPath(String path);

    MenuBo getMenuByAccessCode(String accessCode);

    void removeMenu(Long id);

    List<MenuBo> getMenus(List<Long> menuIds);

    /**
     * 返回所选菜单及其全部有效祖先节点。
     *
     * @param menuIds  所选菜单 ID
     * @param thorough 是否严格校验所选菜单及其层级关系
     */
    List<MenuBo> getMenusWithAncestors(Set<Long> menuIds, boolean thorough);

    List<MenuRouteVo> getMenuRoutes(List<Long> menuIds);
}
