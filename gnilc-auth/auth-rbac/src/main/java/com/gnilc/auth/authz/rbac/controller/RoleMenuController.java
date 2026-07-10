package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 角色关联权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/role-menu")
public class RoleMenuController {
    @Autowired
    private RoleMenuService roleMenuService;

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getMenuIds(@PathVariable("roleId") Long roleId) {
        List<Long> menuIds = roleMenuService.getMenuIds(roleId);

        return R.success(menuIds);
    }

    @PostMapping("/update")
    public R<?> updateRoleMenu(@RequestBody RoleMenuDto rmd) {
        roleMenuService.updateRoleMenu(rmd);

        return R.success();
    }
}
