package com.gnilc.authz.rbac.controller;


import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.vo.MenuVo;
import com.gnilc.authz.rbac.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/ac/menu")
public class MenuController {
    @Autowired
    private MenuService menuService;

    @PostMapping("/tree")
    public R<List<MenuVo>> getMenuTree() {
        List<MenuVo> tree = menuService.getMenuTree();
        return R.success(tree);
    }

    @PostMapping("/save")
    public R<?> saveMenu(@RequestBody MenuDto menuDto) {
        menuService.saveMenu(menuDto);

        return R.success();
    }

    @PostMapping("/modify")
    public R<?> modifyMenu(@RequestBody MenuDto menuDto) {
        menuService.modifyMenu(menuDto);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeMenu(@PathVariable("id") Long id) {
        menuService.removeMenu(id);

        return R.success();
    }

}
