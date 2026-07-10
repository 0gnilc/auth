package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/permission")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping("/list")
    public R<List<PermissionVo>> getPermissions(@RequestBody PermissionQueryDto pqd) {
        List<PermissionVo> pvs = permissionService.getPermissions(pqd);

        return R.success(pvs);
    }

    @PostMapping("/create")
    public R<?> createPermission(@RequestBody PermissionDto pd) {
        permissionService.createPermission(pd);

        return R.success();
    }

    @PostMapping("/update")
    public R<?> updatePermission(@RequestBody PermissionDto pd) {
        permissionService.updatePermission(pd);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removePermission(@PathVariable("id") Long id) {
        permissionService.removePermission(id);

        return R.success();
    }

    @PostMapping("/cache/clear-all")
    public R<?> clearAllPermissionCache() {
        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR));

        return R.success();
    }


}
