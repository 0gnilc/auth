package com.gnilc.auth.authz.rbac.service;

import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.provider.cache.DatabasePermissionCacheLoader;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = RbacTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RbacContainerContextInitializer.class)
@Transactional
class RbacServiceIT {
    @Autowired private RoleService roles;
    @Autowired private PermissionService permissions;
    @Autowired private UserService users;
    @Autowired private UserRoleService userRoles;
    @Autowired private RolePermissionService rolePermissions;
    @Autowired private MenuService menus;
    @Autowired private RoleMenuService roleMenus;
    @Autowired private DatabasePermissionCacheLoader cacheLoader;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void roleLifecycleEnforcesUniqueCodesAndProtectsBuiltInRoles() {
        RoleDto create = role("support", "Support");
        roles.createRole(create);
        RoleBo role = roles.getRoleByCode("support");

        assertThat(role).isNotNull();
        assertThat(role.getBuiltIn()).isFalse();
        assertThatThrownBy(() -> roles.createRole(role("support", "Duplicate")))
                .isInstanceOf(InvalidArgumentException.class);

        RoleDto update = role("support-v2", "Support v2");
        update.setId(role.getId());
        roles.updateRole(update);
        assertThat(roles.getRoleByCode("support-v2").getName()).isEqualTo("Support v2");

        RoleBo builtInRole = new RoleBo();
        builtInRole.setCode("built-in");
        builtInRole.setName("Built in");
        builtInRole.setBuiltIn(true);
        roles.save(builtInRole);
        assertThatThrownBy(() -> {
            RoleDto builtIn = role("built-in", "changed");
            builtIn.setId(builtInRole.getId());
            roles.updateRole(builtIn);
        }).isInstanceOf(IllegalConditionException.class);

        roles.removeRole(role.getId());
        assertThat(roles.getById(role.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select code from az_role where id = ?", String.class, role.getId()))
                .isEqualTo("support-v2_del_" + role.getId());

        roles.createRole(role("support-v2", "Replacement support"));
        assertThat(roles.getRoleByCode("support-v2").getId()).isNotEqualTo(role.getId());
    }

    @Test
    void permissionLifecycleSupportsFilteringAndLogicalRemoval() {
        PermissionDto create = permission("invoice:read", "Read invoices", "/invoices/**", false);
        permissions.createPermission(create);
        PermissionBo stored = permissions.getPermissionByCode("invoice:read");

        PermissionQueryDto query = new PermissionQueryDto();
        query.setTargetIdentifier("invoice");
        assertThat(permissions.getPermissions(query))
                .extracting(com.gnilc.auth.authz.rbac.entity.vo.PermissionVo::getCode)
                .contains("invoice:read");

        PermissionDto update = permission("invoice:view", "View invoices", "/invoices/**", true);
        update.setId(stored.getId());
        permissions.updatePermission(update);
        assertThat(permissions.getPermissionByCode("invoice:view").getPublicAccess()).isTrue();

        permissions.removePermission(stored.getId());
        assertThat(permissions.getById(stored.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select code from az_permission where id = ?", String.class, stored.getId()))
                .isEqualTo("invoice:view_del_" + stored.getId());

        permissions.createPermission(permission(
                "invoice:view", "Replacement invoice view", "/invoices/**", false));
        assertThat(permissions.getPermissionByCode("invoice:view").getId())
                .isNotEqualTo(stored.getId());
    }

    @Test
    void relationshipServicesReplaceBindingsAndExposeUserReadModels() {
        roles.createRole(role("analyst", "Analyst"));
        RoleBo analyst = roles.getRoleByCode("analyst");
        permissions.createPermission(permission("report:read", "Read reports", "/reports/**", false));
        PermissionBo report = permissions.getPermissionByCode("report:read");
        Long userId = users.createUser();

        UserRoleDto userRole = new UserRoleDto();
        userRole.setUserId(userId);
        userRole.setRoleIds(List.of(analyst.getId(), analyst.getId()));
        userRoles.updateUserRole(userRole);
        RolePermissionDto rolePermission = new RolePermissionDto();
        rolePermission.setRoleId(analyst.getId());
        rolePermission.setPermissionIds(List.of(report.getId()));
        rolePermissions.updateRolePermission(rolePermission);

        assertThat(users.checkRole(userId, "analyst")).isTrue();
        assertThat(users.getRoles(userId)).extracting(RoleBo::getCode).containsExactly("analyst");
        assertThat(users.getPermissions(userId)).extracting(PermissionBo::getCode)
                .containsExactly("report:read");

        assertThat(cacheLoader.loadUserPermissions(userId))
                .containsExactly(new Permission("report:read"));
        assertThat(cacheLoader.loadTargetPermissions())
                .contains(new TargetPermission("/reports/**", "report:read"));

        userRole.setRoleIds(List.of());
        userRoles.updateUserRole(userRole);
        assertThat(users.getRoles(userId)).isEmpty();
    }

    @Test
    void relationshipServicesHidePhysicalDuplicatesAndStillReplaceSets() {
        roles.createRole(role("duplicate-relations", "Duplicate relations"));
        RoleBo role = roles.getRoleByCode("duplicate-relations");
        permissions.createPermission(permission(
                "duplicates:read", "Read duplicates", "/duplicates/**", false));
        PermissionBo permission = permissions.getPermissionByCode("duplicates:read");
        MenuDto menuDto = menu("duplicates", "Duplicates", "/duplicates", MenuType.MENU, 0L, 10);
        menuDto.setComponent("views/duplicates");
        menus.createMenu(menuDto);
        MenuBo menu = menus.getMenuByPath("/duplicates");
        Long userId = users.createUser();

        UserRoleDto userRole = new UserRoleDto();
        userRole.setUserId(userId);
        userRole.setRoleIds(List.of(role.getId()));
        userRoles.updateUserRole(userRole);
        RolePermissionDto rolePermission = new RolePermissionDto();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionIds(List.of(permission.getId()));
        rolePermissions.updateRolePermission(rolePermission);
        RoleMenuDto roleMenu = new RoleMenuDto();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuIds(List.of(menu.getId()));
        roleMenus.updateRoleMenu(roleMenu);

        jdbc.update("""
                insert into az_user_role (del, create_time, user_id, role_id)
                values (0, now(), ?, ?)
                """, userId, role.getId());
        jdbc.update("""
                insert into az_role_permission (del, create_time, role_id, permission_id)
                values (0, now(), ?, ?)
                """, role.getId(), permission.getId());
        jdbc.update("""
                insert into az_role_menu (del, create_time, role_id, menu_id)
                values (0, now(), ?, ?)
                """, role.getId(), menu.getId());

        assertThat(activeRelationCount("az_user_role", "user_id", userId)).isEqualTo(2);
        assertThat(activeRelationCount("az_role_permission", "role_id", role.getId())).isEqualTo(2);
        assertThat(activeRelationCount("az_role_menu", "role_id", role.getId())).isEqualTo(2);
        assertThat(userRoles.getRoleIds(userId)).containsExactly(role.getId());
        assertThat(userRoles.getUserIds(role.getId())).containsExactly(userId);
        assertThat(rolePermissions.getPermissionIds(role.getId())).containsExactly(permission.getId());
        assertThat(rolePermissions.getRoleIds(permission.getId())).containsExactly(role.getId());
        assertThat(roleMenus.getMenuIds(role.getId())).containsExactly(menu.getId());

        userRole.setRoleIds(List.of());
        userRoles.updateUserRole(userRole);
        rolePermission.setPermissionIds(List.of());
        rolePermissions.updateRolePermission(rolePermission);
        roleMenu.setMenuIds(List.of());
        roleMenus.updateRoleMenu(roleMenu);
        assertThat(userRoles.getRoleIds(userId)).isEmpty();
        assertThat(rolePermissions.getPermissionIds(role.getId())).isEmpty();
        assertThat(roleMenus.getMenuIds(role.getId())).isEmpty();

        userRole.setRoleIds(List.of(role.getId()));
        userRoles.updateUserRole(userRole);
        rolePermission.setPermissionIds(List.of(permission.getId()));
        rolePermissions.updateRolePermission(rolePermission);
        roleMenu.setMenuIds(List.of(menu.getId()));
        roleMenus.updateRoleMenu(roleMenu);
        assertThat(userRoles.getRoleIds(userId)).containsExactly(role.getId());
        assertThat(rolePermissions.getPermissionIds(role.getId())).containsExactly(permission.getId());
        assertThat(roleMenus.getMenuIds(role.getId())).containsExactly(menu.getId());
    }

    @Test
    void menuTreeAndRoleMenuBindingsPreserveHierarchyAndOrder() {
        MenuDto root = menu("workspace", "Workspace", "/workspace", MenuType.CATALOG, 0L, 20);
        menus.createMenu(root);
        MenuBo rootBo = menus.getMenuByPath("/workspace");
        MenuDto child = menu("dashboard", "Dashboard", "/dashboard", MenuType.MENU, rootBo.getId(), 5);
        child.setComponent("views/dashboard");
        menus.createMenu(child);
        MenuDto button = menu("dashboard-edit", "Edit", null, MenuType.BUTTON, rootBo.getId(), 1);
        button.setAccessCode("dashboard:edit");
        menus.createMenu(button);

        assertThat(menus.getMenuTree()).filteredOn(vo -> "workspace".equals(vo.getName()))
                .singleElement()
                .satisfies(vo -> assertThat(vo.getChildren())
                        .extracting(com.gnilc.auth.authz.rbac.entity.vo.MenuVo::getName)
                        .containsExactly("dashboard-edit", "dashboard"));

        roles.createRole(role("editor", "Editor"));
        RoleBo editor = roles.getRoleByCode("editor");
        RoleMenuDto binding = new RoleMenuDto();
        binding.setRoleId(editor.getId());
        binding.setMenuIds(List.of(rootBo.getId(), menus.getMenuByAccessCode("dashboard:edit").getId()));
        roleMenus.updateRoleMenu(binding);

        assertThat(roleMenus.getMenuIds(editor.getId())).hasSize(2);
    }

    @Test
    void publicPermissionLoaderReturnsOnlyPublicEntries() {
        permissions.createPermission(permission("public:health", "Health", "/health", true));
        permissions.createPermission(permission("private:health", "Private health", "/private-health", false));

        assertThat(cacheLoader.loadPublicAccessPermissions())
                .contains(new Permission("public:health"))
                .doesNotContain(new Permission("private:health"));
    }

    private RoleDto role(String code, String name) {
        RoleDto dto = new RoleDto();
        dto.setCode(code);
        dto.setName(name);
        return dto;
    }

    private PermissionDto permission(String code, String name, String target, boolean publicAccess) {
        PermissionDto dto = new PermissionDto();
        dto.setCode(code);
        dto.setName(name);
        dto.setTargetIdentifier(target);
        dto.setPublicAccess(publicAccess);
        return dto;
    }

    private MenuDto menu(String name, String title, String path, MenuType type, long pid, int order) {
        MenuDto dto = new MenuDto();
        dto.setName(name);
        dto.setTitle(title);
        dto.setPath(path);
        dto.setType(type);
        dto.setPid(pid);
        dto.setOrder(order);
        dto.setStatus(true);
        return dto;
    }

    private int activeRelationCount(String table, String ownerColumn, Long ownerId) {
        return jdbc.queryForObject(
                "select count(*) from " + table + " where " + ownerColumn + " = ? and del = 0",
                Integer.class,
                ownerId);
    }
}
