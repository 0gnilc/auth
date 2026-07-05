package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 角色关联权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/role-permission")
public class RolePermissionController {
    @Autowired
    private RolePermissionService rolePermissionService;

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getPermissionIds(@PathVariable("roleId") Long roleId) {
        List<Long> permissionIds = rolePermissionService.getPermissionIds(roleId);
        return R.success(permissionIds);
    }

    @PostMapping("/update")
    public R<?> updateRolePermission(@RequestBody RolePermissionDto rpd) {
        rolePermissionService.updateRolePermission(rpd);

        return R.success();
    }
}
