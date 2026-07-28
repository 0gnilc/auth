package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
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
@RequestMapping("/authz/role-permission")
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @PostMapping("/list/{roleId}")
    public R<List<Long>> getPermissionIds(@PathVariable("roleId") Long roleId) {
        return R.success(rolePermissionService.getPermissionIds(roleId));
    }

    @PostMapping("/save")
    public R<?> saveRolePermissions(@RequestBody RolePermissionDto dto) {
        rolePermissionService.saveRolePermissions(dto);

        return R.success();
    }
}
