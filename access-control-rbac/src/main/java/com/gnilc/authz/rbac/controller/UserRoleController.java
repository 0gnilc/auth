package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.authz.rbac.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authz/user-role")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    @PostMapping("/list/{userId}")
    public R<List<Long>> getRoleIds(@PathVariable("userId") Long userId) {
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        return R.success(roleIds);
    }

    @PostMapping("/update")
    public R<?> updateUserRole(@RequestBody UserRoleDto urd) {
        userRoleService.updateUserRole(urd);

        return R.success();
    }
}
