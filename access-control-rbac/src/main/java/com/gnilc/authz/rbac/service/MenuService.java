package com.gnilc.authz.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.vo.MenuVo;

import java.util.List;

public interface MenuService extends IService<MenuBo> {

    List<MenuVo> getMenuTree();

    void createMenu(MenuDto md);

    void updateMenu(MenuDto md);

    MenuBo getMenuByPath(String path);

    MenuBo getMenuByAccessCode(String accessCode);

    void removeMenu(Long id);

    List<MenuBo> getMenus(Long userId);

    List<MenuBo> getMenus(List<Long> menuIds);
}
