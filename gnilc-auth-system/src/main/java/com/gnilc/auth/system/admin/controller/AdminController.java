package com.gnilc.auth.system.admin.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.system.admin.entity.dto.AdminDto;
import com.gnilc.auth.system.admin.entity.dto.AdminPageDto;
import com.gnilc.auth.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.auth.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.auth.system.admin.entity.vo.AdminVo;
import com.gnilc.auth.system.admin.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理员 API。
 */
@RestController
@RequestMapping("/sys/admin")
public class AdminController {
    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 分页查询管理员。
     */
    @PostMapping("/page")
    public R<PageResult<AdminVo>> getAdminPage(@RequestBody(required = false) AdminPageDto dto) {
        return R.success(adminService.getAdminPage(dto));
    }

    /**
     * 创建管理员。
     */
    @PostMapping("/create")
    public R<?> createAdmin(@RequestBody AdminDto dto) {
        adminService.createAdmin(dto);
        return R.success();
    }

    /**
     * 更新管理员资料。
     */
    @PostMapping("/update")
    public R<?> updateAdmin(@RequestBody AdminDto dto) {
        adminService.updateAdmin(dto);
        return R.success();
    }

    /**
     * 替换管理员角色。
     */
    @PostMapping("/update-roles")
    public R<?> updateAdminRoles(@RequestBody AdminRoleDto dto) {
        adminService.updateAdminRoles(dto);
        return R.success();
    }

    /**
     * 删除管理员。
     */
    @PostMapping("/remove/{id}")
    public R<?> removeAdmin(@PathVariable("id") Long id) {
        adminService.removeAdmin(id);
        return R.success();
    }

    /**
     * 登录管理员。
     */
    @PostMapping("/login")
    public R<AdminTokenVo> login(@RequestBody(required = false) JSONObject body) {
        String username = body == null ? null : body.getString("username");
        String password = body == null ? null : body.getString("password");
        AdminTokenVo token = adminService.login(username, password);
        if (token == null) {
            return R.error(ResponseCode.AUTHENTICATION_FAILED, "用户名或密码错误");
        }
        return R.success(token);
    }

    /**
     * 刷新访问令牌。
     */
    @PostMapping("/refresh")
    public ResponseEntity<R<?>> refresh(@RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return unauthorized();
        }
        AdminTokenVo token = adminService.refresh(refreshToken);
        if (token == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(R.success(token));
    }

    /**
     * 登出当前会话。
     */
    @PostMapping("/logout")
    public ResponseEntity<R<?>> logout(@RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return unauthorized();
        }
        if (!adminService.logout(refreshToken)) {
            return unauthorized();
        }
        return ResponseEntity.ok(R.success());
    }

    /**
     * 查询当前管理员资料。
     */
    @GetMapping("/user-info")
    public R<AdminVo> getAdminUserInfo() {
        return R.success(adminService.getUserInfo());
    }

    /**
     * 查询当前管理员角色标识。
     */
    @GetMapping("/role-codes")
    public R<List<String>> getRoleCodes() {
        return R.success(adminService.getRoleCodes());
    }

    /**
     * 查询当前管理员按钮访问标识。
     */
    @GetMapping("/menu/access-codes")
    public R<List<String>> getMenuAccessCodes() {
        return R.success(adminService.getMenuAccessCodes());
    }

    private ResponseEntity<R<?>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.error(ResponseCode.UNAUTHORIZED, "unauthorized"));
    }
}
