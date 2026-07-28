package com.gnilc.auth.authz.rbac.controller;


import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/menu")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/tree")
    public R<List<MenuVo>> getMenuTree() {
        return R.success(menuService.getMenuTree());
    }

    @PostMapping("/create")
    public R<?> createMenu(@RequestBody MenuDto dto) {
        menuService.createMenu(dto);

        return R.success();
    }

    /**
     * 完整更新菜单，未提交的可空菜单属性会被清空。
     */
    @PostMapping("/update")
    public R<?> updateMenu(@RequestBody MenuDto dto) {
        menuService.updateMenu(dto);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeMenu(@PathVariable("id") Long id) {
        menuService.removeMenu(id);

        return R.success();
    }

}
