package com.gnilc.authz.example.controller;

import com.gnilc.authz.rbac.common.utils.R;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/example")
@Profile({"dev", "localtest"})
public class ExampleProtectedController {

    @GetMapping("/public/ping")
    public R<Map<String, String>> publicPing() {
        return R.success(Map.of("message", "public pong"));
    }

    @GetMapping("/protected/ping")
    public R<Map<String, String>> protectedPing() {
        return R.success(Map.of("message", "protected pong"));
    }
}
