package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/authz/user-role")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PostMapping("/list/{userId}")
    public R<List<Long>> getRoleIds(@PathVariable("userId") Long userId) {
        return R.success(userRoleService.getRoleIds(userId));
    }

    @PostMapping("/update")
    public R<?> updateUserRole(@RequestBody UserRoleDto dto) {
        userRoleService.updateUserRole(dto);

        return R.success();
    }
}
