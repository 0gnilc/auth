package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.authz.rbac.service.RoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 角色关联权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/ac/role-menu")
public class RoleMenuController {
    @Autowired
    private RoleMenuService roleMenuService;

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getMenuIds(@PathVariable("roleId") Long roleId) {
        List<Long> menuIds = roleMenuService.getMenuIds(roleId);

        return R.success(menuIds);
    }

    @PostMapping("/save")
    public R<?> saveRoleMenu(@RequestBody RoleMenuDto roleMenuDto) {
        roleMenuService.saveRoleMenu(roleMenuDto);

        return R.success();
    }
}
