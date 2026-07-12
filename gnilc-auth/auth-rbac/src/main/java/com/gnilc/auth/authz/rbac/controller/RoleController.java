package com.gnilc.auth.authz.rbac.controller;


import com.gnilc.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.common.utils.R;

import java.util.List;

/**
 * 角色
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/role")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/page")
    public R<PageResult<RoleVo>> getRolePage(@RequestBody RolePageDto dto) {
        return R.success(roleService.getRolePage(dto));
    }

    @PostMapping("/list")
    public R<List<RoleVo>> getRoles(@RequestBody RoleQueryDto dto) {
        return R.success(roleService.getRoles(dto));
    }

    @PostMapping("/create")
    public R<?> createRole(@RequestBody RoleDto dto) {
        roleService.createRole(dto);

        return R.success();
    }

    @PostMapping("/update")
    public R<?> updateRole(@RequestBody RoleDto dto) {
        roleService.updateRole(dto);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeRole(@PathVariable("id") Long id) {
        roleService.removeRole(id);

        return R.success();
    }

}
