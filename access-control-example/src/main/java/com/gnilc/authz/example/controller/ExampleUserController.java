package com.gnilc.authz.example.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/example/users")
@Profile({"dev", "localtest"})
public class ExampleUserController {
    private final UserService userService;

    public ExampleUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public R<Long> createUser() {
        return R.success(userService.createUser());
    }

    @DeleteMapping("/{id}")
    public R<?> removeUser(@PathVariable("id") Long id) {
        userService.removeUser(id);
        return R.success();
    }
}
