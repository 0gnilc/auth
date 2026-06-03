package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.authz.rbac.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 角色关联权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/ac/role-permission")
public class RolePermissionController {
    @Autowired
    private RolePermissionService rolePermissionService;

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getPermissionIds(@PathVariable("roleId") Long roleId) {
        List<Long> permissionIds = rolePermissionService.getPermissionIds(roleId);
        return R.success(permissionIds);
    }

    @PostMapping("/save")
    public R<?> saveRolePermission(@RequestBody RolePermissionDto rolePermissionDto) {
        rolePermissionService.saveRolePermission(rolePermissionDto);

        return R.success();
    }
}
