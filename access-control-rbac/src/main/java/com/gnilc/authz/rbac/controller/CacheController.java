package com.gnilc.authz.rbac.controller;

import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ac/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @PostMapping("/clear")
    public R<?> clear() {
        cacheService.clear();
        return R.success();
    }
}
