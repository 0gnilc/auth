package com.gnilc.auth.authz.rbac.api;

import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@ApiTest
@Import(RestExceptionHandlingConfiguration.class)
@ContextConfiguration(
        classes = RbacTestApplication.class,
        initializers = RbacContainerContextInitializer.class)
class RbacAssignmentApiIT extends ApiTestSupport {
    @Autowired private RoleService roles;
    @Autowired private PermissionService permissions;
    @Autowired private MenuService menus;
    @Autowired private UserService users;

    @Test
    void assignmentApisDeduplicateReplaceClearAndPreserveBindingsAfterInvalidInput() {
        RoleBo role = createRole();
        PermissionBo permission = createPermission();
        MenuBo catalog = createMenu("Reports", "/reports", MenuType.CATALOG, 0L, null);
        MenuBo button = createMenu("ReportsExport", null, MenuType.BUTTON,
                catalog.getId(), "reports:export");
        Long userId = users.createUser();

        post("/authz/user-role/update",
                Map.of("userId", userId, "roleIds", List.of(role.getId(), role.getId())))
                .body("code", equalTo(0));
        post("/authz/role-permission/save",
                Map.of("roleId", role.getId(),
                        "permissionIds", List.of(permission.getId(), permission.getId())))
                .body("code", equalTo(0));
        post("/authz/role-menu/save",
                Map.of("roleId", role.getId(),
                        "menuIds", List.of(button.getId(), button.getId())))
                .body("code", equalTo(0));

        post("/authz/user-role/list/" + userId, Map.of())
                .body("data", contains(role.getId().toString()));
        post("/authz/role-permission/list/" + role.getId(), Map.of())
                .body("data", contains(permission.getId().toString()));
        post("/authz/role-menu/list/" + role.getId(), Map.of())
                .body("data", contains(catalog.getId().toString(), button.getId().toString()));

        post("/authz/role-permission/save",
                Map.of("roleId", role.getId(), "permissionIds", List.of(Long.MAX_VALUE)))
                .body("code", equalTo(10002));
        post("/authz/role-permission/list/" + role.getId(), Map.of())
                .body("data", contains(permission.getId().toString()));

        post("/authz/user-role/update", Map.of("userId", userId, "roleIds", List.of()))
                .body("code", equalTo(0));
        post("/authz/role-permission/save",
                Map.of("roleId", role.getId(), "permissionIds", List.of()))
                .body("code", equalTo(0));
        post("/authz/role-menu/save",
                Map.of("roleId", role.getId(), "menuIds", List.of()))
                .body("code", equalTo(0));

        post("/authz/user-role/list/" + userId, Map.of()).body("data", empty());
        post("/authz/role-permission/list/" + role.getId(), Map.of()).body("data", empty());
        post("/authz/role-menu/list/" + role.getId(), Map.of()).body("data", empty());
    }

    private io.restassured.response.ValidatableResponse post(String path, Object body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then()
                .statusCode(200);
    }

    private RoleBo createRole() {
        RoleDto dto = new RoleDto();
        dto.setCode("reporter");
        dto.setName("Reporter");
        roles.createRole(dto);
        return roles.getRoleByCode(dto.getCode());
    }

    private PermissionBo createPermission() {
        PermissionDto dto = new PermissionDto();
        dto.setCode("reports:read");
        dto.setName("Read reports");
        dto.setTargetIdentifier("/reports/**");
        dto.setPublicAccess(false);
        permissions.createPermission(dto);
        return permissions.getPermissionByCode(dto.getCode());
    }

    private MenuBo createMenu(String name, String path, MenuType type,
                              Long parentId, String accessCode) {
        MenuDto dto = new MenuDto();
        dto.setName(name);
        dto.setTitle(name);
        dto.setPath(path);
        dto.setType(type);
        dto.setPid(parentId);
        dto.setAccessCode(accessCode);
        dto.setStatus(true);
        dto.setOrder(1);
        menus.createMenu(dto);
        return accessCode == null
                ? menus.getMenuByPath(path)
                : menus.getMenuByAccessCode(accessCode);
    }
}
