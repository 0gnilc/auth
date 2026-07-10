package com.gnilc.auth.authz.rbac.controller;


import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/menu")
public class MenuController {
    @Autowired
    private MenuService menuService;

    @PostMapping("/tree")
    public R<List<MenuVo>> getMenuTree() {
        List<MenuVo> mvs = menuService.getMenuTree();
        return R.success(mvs);
    }

    @PostMapping("/create")
    public R<?> createMenu(@RequestBody MenuDto md) {
        menuService.createMenu(md);

        return R.success();
    }

    @PostMapping("/update")
    public R<?> updateMenu(@RequestBody MenuDto md) {
        menuService.updateMenu(md);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeMenu(@PathVariable("id") Long id) {
        menuService.removeMenu(id);

        return R.success();
    }

}
