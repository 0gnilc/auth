package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 权限
 *
 * @author kyhns7
 */
@RestController
@RequestMapping("/authz/permission")
public class PermissionController {
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;

    public PermissionController(PermissionService permissionService,
                                ApplicationEventPublisher eventPublisher) {
        this.permissionService = permissionService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/list")
    public R<List<PermissionVo>> getPermissions(@RequestBody PermissionQueryDto dto) {
        return R.success(permissionService.getPermissions(dto));
    }

    @PostMapping("/create")
    public R<?> createPermission(@RequestBody PermissionDto dto) {
        permissionService.createPermission(dto);

        return R.success();
    }

    @PostMapping("/update")
    public R<?> updatePermission(@RequestBody PermissionDto dto) {
        permissionService.updatePermission(dto);

        return R.success();
    }

    @PostMapping("/remove/{id}")
    public R<?> removePermission(@PathVariable("id") Long id) {
        permissionService.removePermission(id);

        return R.success();
    }

    @PostMapping("/cache/clear-all")
    public R<?> clearAllPermissionCache() {
        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR));

        return R.success();
    }


}
