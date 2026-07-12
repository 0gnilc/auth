package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 角色关联权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/role-menu")
public class RoleMenuController {
    private final RoleMenuService roleMenuService;

    public RoleMenuController(RoleMenuService roleMenuService) {
        this.roleMenuService = roleMenuService;
    }

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getMenuIds(@PathVariable("roleId") Long roleId) {
        return R.success(roleMenuService.getMenuIds(roleId));
    }

    @PostMapping("/update")
    public R<?> updateRoleMenu(@RequestBody RoleMenuDto dto) {
        roleMenuService.updateRoleMenu(dto);

        return R.success();
    }
}
