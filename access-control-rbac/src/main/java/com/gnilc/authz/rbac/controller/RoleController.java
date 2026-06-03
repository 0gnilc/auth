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
@RequestMapping("/ac/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping("/page")
    public R<PageResult<RoleVo>> getRolePage(@RequestBody RolePageDto pd) {
        PageResult<RoleVo> page = roleService.getRolePage(pd);

        return R.success(page);
    }

    @PostMapping("/list")
    public R<List<RoleVo>> getRoles(@RequestBody RoleQueryDto qd) {
        List<RoleVo> list = roleService.getRoles(qd);

        return R.success(list);
    }

    @PostMapping("/save")
    public R<?> saveRole(@RequestBody RoleDto roleDto) {
        roleService.saveRole(roleDto);

        return R.success();
    }

    @PostMapping("/modify")
    public R<?> modifyRole(@RequestBody RoleDto roleDto) {
        roleService.modifyRole(roleDto);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removeRole(@PathVariable("id") Long id) {
        roleService.removeRole(id);

        return R.success();
    }

}
