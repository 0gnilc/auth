package com.gnilc.authz.rbac.controller;


import com.gnilc.authz.rbac.common.utils.PageResult;
import com.gnilc.authz.rbac.entity.dto.RoleDto;
import com.gnilc.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.authz.rbac.entity.vo.RoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.gnilc.authz.rbac.service.RoleService;
import com.gnilc.authz.rbac.common.utils.R;

import java.util.List;

/**
 * 角色
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping("/page")
    public R<PageResult<RoleVo>> getRolePage(@RequestBody RolePageDto rd) {
        PageResult<RoleVo> page = roleService.getRolePage(rd);

        return R.success(page);
    }

    @PostMapping("/list")
    public R<List<RoleVo>> getRoles(@RequestBody RoleQueryDto rd) {
        List<RoleVo> rvs = roleService.getRoles(rd);

        return R.success(rvs);
    }

    @PostMapping("/create")
    public R<?> createRole(@RequestBody RoleDto rd) {
        roleService.createRole(rd);

        return R.success();
    }

    @PostMapping("/update")
    public R<?> updateRole(@RequestBody RoleDto rd) {
        roleService.updateRole(rd);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeRole(@PathVariable("id") Long id) {
        roleService.removeRole(id);

        return R.success();
    }

}
