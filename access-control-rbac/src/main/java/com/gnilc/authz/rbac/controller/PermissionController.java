package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.authz.rbac.entity.vo.PermissionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.gnilc.authz.rbac.service.PermissionService;

import java.util.List;


/**
 * 权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/ac/permission")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/list")
    public R<List<PermissionVo>> getPermissions(@RequestBody PermissionQueryDto qd) {
        List<PermissionVo> list = permissionService.getPermissions(qd);

        return R.success(list);
    }

    @PostMapping("/save")
    public R<?> savePermission(@RequestBody PermissionDto pd) {
        permissionService.savePermission(pd);

        return R.success();
    }

    @PostMapping("/modify")
    public R<?> modifyPermission(@RequestBody PermissionDto pd) {
        permissionService.modifyPermission(pd);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removePermission(@PathVariable("id") Long id) {
        permissionService.removePermission(id);

        return R.success();
    }


}
