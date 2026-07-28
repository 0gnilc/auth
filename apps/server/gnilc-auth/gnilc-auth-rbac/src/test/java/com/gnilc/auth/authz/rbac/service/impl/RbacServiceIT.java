package com.gnilc.auth.authz.rbac.service.impl;

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
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.provider.cache.DatabasePermissionCacheLoader;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

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
        create.setRemark("Temporary remark");
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
        assertThat(roles.getRoleByCode("support-v2").getRemark()).isNull();

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
        create.setTargetQualifier("GET");
        create.setRemark("Temporary remark");
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
        assertThat(permissions.getPermissionByCode("invoice:view").getTargetQualifier()).isNull();
        assertThat(permissions.getPermissionByCode("invoice:view").getRemark()).isNull();

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
        rolePermissions.saveRolePermissions(rolePermission);

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
    void roleRemovalClearsAllBindingsAndUserGrants() {
        roles.createRole(role("temporary-operator", "Temporary operator"));
        RoleBo role = roles.getRoleByCode("temporary-operator");
        permissions.createPermission(permission(
                "temporary:operate", "Temporary operation", "/temporary/**", false));
        PermissionBo permission = permissions.getPermissionByCode("temporary:operate");
        MenuDto menuDto = menu("temporary", "Temporary", "/temporary", MenuType.CATALOG, 0L, 10);
        menus.createMenu(menuDto);
        MenuBo menu = menus.getMenuByPath("/temporary");
        Long userId = users.createUser();

        UserRoleDto userRole = new UserRoleDto();
        userRole.setUserId(userId);
        userRole.setRoleIds(List.of(role.getId()));
        userRoles.updateUserRole(userRole);
        RolePermissionDto rolePermission = new RolePermissionDto();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionIds(List.of(permission.getId()));
        rolePermissions.saveRolePermissions(rolePermission);
        RoleMenuDto roleMenu = new RoleMenuDto();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuIds(List.of(menu.getId()));
        roleMenus.saveRoleMenus(roleMenu);

        roles.removeRole(role.getId());

        assertThat(userRoles.getRoleIds(userId)).isEmpty();
        assertThat(rolePermissions.getPermissionIds(role.getId())).isEmpty();
        assertThat(roleMenus.getMenuIds(role.getId())).isEmpty();
        assertThat(users.getPermissions(userId)).isEmpty();
        assertThat(users.getMenus(userId)).isEmpty();
    }

    @Test
    void permissionRemovalClearsRoleBindingsAndUserGrants() {
        roles.createRole(role("permission-owner", "Permission owner"));
        RoleBo role = roles.getRoleByCode("permission-owner");
        permissions.createPermission(permission(
                "temporary:read", "Read temporary data", "/temporary/**", false));
        PermissionBo permission = permissions.getPermissionByCode("temporary:read");
        Long userId = users.createUser();
        UserRoleDto userRole = new UserRoleDto();
        userRole.setUserId(userId);
        userRole.setRoleIds(List.of(role.getId()));
        userRoles.updateUserRole(userRole);
        RolePermissionDto rolePermission = new RolePermissionDto();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionIds(List.of(permission.getId()));
        rolePermissions.saveRolePermissions(rolePermission);

        permissions.removePermission(permission.getId());

        assertThat(rolePermissions.getPermissionIds(role.getId())).isEmpty();
        assertThat(users.getPermissions(userId)).isEmpty();
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
        rolePermissions.saveRolePermissions(rolePermission);
        RoleMenuDto roleMenu = new RoleMenuDto();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuIds(List.of(menu.getId()));
        roleMenus.saveRoleMenus(roleMenu);

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
        assertThat(users.getRoles(userId)).extracting(RoleBo::getCode)
                .containsExactly("duplicate-relations");
        assertThat(users.getPermissions(userId)).extracting(PermissionBo::getCode)
                .containsExactly("duplicates:read");
        assertThat(users.getMenus(userId)).extracting(MenuBo::getName)
                .containsExactly("duplicates");

        userRoles.updateUserRole(userRole);
        rolePermissions.saveRolePermissions(rolePermission);
        roleMenus.saveRoleMenus(roleMenu);
        assertThat(activeRelationCount("az_user_role", "user_id", userId)).isEqualTo(2);
        assertThat(activeRelationCount("az_role_permission", "role_id", role.getId())).isEqualTo(2);
        assertThat(activeRelationCount("az_role_menu", "role_id", role.getId())).isEqualTo(2);
        assertThat(userRoles.getRoleIds(userId)).containsExactly(role.getId());
        assertThat(rolePermissions.getPermissionIds(role.getId())).containsExactly(permission.getId());
        assertThat(roleMenus.getMenuIds(role.getId())).containsExactly(menu.getId());

        userRole.setRoleIds(List.of());
        userRoles.updateUserRole(userRole);
        rolePermission.setPermissionIds(List.of());
        rolePermissions.saveRolePermissions(rolePermission);
        roleMenu.setMenuIds(List.of());
        roleMenus.saveRoleMenus(roleMenu);
        assertThat(userRoles.getRoleIds(userId)).isEmpty();
        assertThat(rolePermissions.getPermissionIds(role.getId())).isEmpty();
        assertThat(roleMenus.getMenuIds(role.getId())).isEmpty();

        userRole.setRoleIds(List.of(role.getId()));
        userRoles.updateUserRole(userRole);
        rolePermission.setPermissionIds(List.of(permission.getId()));
        rolePermissions.saveRolePermissions(rolePermission);
        roleMenu.setMenuIds(List.of(menu.getId()));
        roleMenus.saveRoleMenus(roleMenu);
        assertThat(userRoles.getRoleIds(userId)).containsExactly(role.getId());
        assertThat(rolePermissions.getPermissionIds(role.getId())).containsExactly(permission.getId());
        assertThat(roleMenus.getMenuIds(role.getId())).containsExactly(menu.getId());
    }

    @ParameterizedTest(name = "{0} parent -> {2}, accepted: {3}")
    @MethodSource("menuParentChildCases")
    void menuHierarchyEnforcesEveryParentChildTypeCombination(
            String parentLabel,
            MenuType parentType,
            MenuType childType,
            boolean accepted) {
        long parentId = createMatrixParent(parentType);
        MenuDto child = validMenu("matrix-child", childType, parentId);

        if (accepted) {
            menus.createMenu(child);
            assertThat(findMenu(child)).isNotNull();
        } else {
            assertThatThrownBy(() -> menus.createMenu(child))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @ParameterizedTest(name = "{0} URL length {1}, accepted: {2}")
    @MethodSource("menuUrlBoundaries")
    void menuHttpUrlLengthUsesTheExactFiveHundredCharacterBoundary(
            MenuType type,
            int length,
            boolean accepted) {
        MenuDto menu = validMenu("url-" + type.name().toLowerCase() + '-' + length, type, 0L);
        String url = httpUrl(length);
        if (type == MenuType.EMBEDDED) {
            menu.setIframeSrc(url);
        } else {
            menu.setLink(url);
        }

        if (accepted) {
            menus.createMenu(menu);
            MenuBo stored = menus.getMenuByPath(menu.getPath());
            assertThat(type == MenuType.EMBEDDED ? stored.getIframeSrc() : stored.getLink())
                    .hasSize(500);
        } else {
            assertThatThrownBy(() -> menus.createMenu(menu))
                    .isInstanceOf(InvalidArgumentException.class);
        }
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
        Long buttonId = menus.getMenuByAccessCode("dashboard:edit").getId();
        binding.setMenuIds(List.of(buttonId));
        roleMenus.saveRoleMenus(binding);

        assertThat(roleMenus.getMenuIds(editor.getId()))
                .containsExactlyInAnyOrder(rootBo.getId(), buttonId);
    }

    @Test
    void roleMenuBindingsRejectMissingMenusWithoutReplacingTheExistingSet() {
        MenuDto menu = menu("reports", "Reports", "/reports", MenuType.CATALOG, 0L, 10);
        menus.createMenu(menu);
        Long menuId = menus.getMenuByPath("/reports").getId();
        roles.createRole(role("reporter", "Reporter"));
        Long roleId = roles.getRoleByCode("reporter").getId();
        RoleMenuDto binding = new RoleMenuDto();
        binding.setRoleId(roleId);
        binding.setMenuIds(List.of(menuId));
        roleMenus.saveRoleMenus(binding);

        binding.setMenuIds(List.of(Long.MAX_VALUE));

        assertThatThrownBy(() -> roleMenus.saveRoleMenus(binding))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(roleMenus.getMenuIds(roleId)).containsExactly(menuId);
    }

    @Test
    void roleMenuBindingsPreserveDisabledMenus() {
        MenuDto disabled = menu("archived-reports", "Archived reports", "/archived-reports",
                MenuType.CATALOG, 0L, 10);
        disabled.setStatus(false);
        menus.createMenu(disabled);
        Long disabledMenuId = menus.getMenuByPath("/archived-reports").getId();
        roles.createRole(role("archivist", "Archivist"));
        Long roleId = roles.getRoleByCode("archivist").getId();
        RoleMenuDto binding = new RoleMenuDto();
        binding.setRoleId(roleId);
        binding.setMenuIds(List.of(disabledMenuId));

        roleMenus.saveRoleMenus(binding);

        assertThat(roleMenus.getMenuIds(roleId)).containsExactly(disabledMenuId);
    }

    @Test
    void roleMenuBindingsRejectDeletedMenusWithoutReplacingTheExistingSet() {
        MenuDto existing = menu("current-reports", "Current reports", "/current-reports",
                MenuType.CATALOG, 0L, 10);
        menus.createMenu(existing);
        Long existingMenuId = menus.getMenuByPath("/current-reports").getId();
        MenuDto deleted = menu("deleted-reports", "Deleted reports", "/deleted-reports",
                MenuType.CATALOG, 0L, 20);
        menus.createMenu(deleted);
        Long deletedMenuId = menus.getMenuByPath("/deleted-reports").getId();
        roles.createRole(role("report-owner", "Report owner"));
        Long roleId = roles.getRoleByCode("report-owner").getId();
        RoleMenuDto binding = new RoleMenuDto();
        binding.setRoleId(roleId);
        binding.setMenuIds(List.of(existingMenuId));
        roleMenus.saveRoleMenus(binding);
        jdbc.update("update az_menu set del = 1 where id = ?", deletedMenuId);

        binding.setMenuIds(List.of(deletedMenuId));

        assertThatThrownBy(() -> roleMenus.saveRoleMenus(binding))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(roleMenus.getMenuIds(roleId)).containsExactly(existingMenuId);
    }

    @Test
    void menuUpdateRejectsMovingAParentBelowItsDescendant() {
        MenuDto root = menu("settings", "Settings", "/settings", MenuType.CATALOG, 0L, 10);
        menus.createMenu(root);
        MenuBo rootBo = menus.getMenuByPath("/settings");
        MenuDto child = menu("users", "Users", "/settings/users", MenuType.MENU, rootBo.getId(), 10);
        child.setComponent("/settings/users/index");
        menus.createMenu(child);

        MenuDto update = new MenuDto();
        update.setId(rootBo.getId());
        update.setPid(menus.getMenuByPath("/settings/users").getId());

        assertThatThrownBy(() -> menus.updateMenu(update))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(menus.getById(rootBo.getId()).getPid()).isZero();
    }

    @Test
    void menuUpdateClearsNullableRoutingOptions() {
        MenuDto create = menu("Reports", "Reports", "/reports", MenuType.MENU, 0L, 10);
        create.setComponent("/reports/index");
        create.setRedirect("/reports/overview");
        create.setAffixTab(true);
        create.setAffixTabOrder(3);
        create.setMaxNumOfOpenTab(2);
        menus.createMenu(create);
        MenuBo stored = menus.getMenuByPath("/reports");

        MenuDto update = menu("Reports", "Reports", "/reports", MenuType.MENU, 0L, 10);
        update.setId(stored.getId());
        update.setName(" Reports ");
        update.setPath(" /reports ");
        update.setComponent(" /reports/index ");
        update.setAffixTab(false);
        menus.updateMenu(update);

        MenuBo updated = menus.getById(stored.getId());
        assertThat(updated.getName()).isEqualTo("Reports");
        assertThat(updated.getPath()).isEqualTo("/reports");
        assertThat(updated.getComponent()).isEqualTo("/reports/index");
        assertThat(updated.getRedirect()).isNull();
        assertThat(updated.getAffixTabOrder()).isNull();
        assertThat(updated.getMaxNumOfOpenTab()).isNull();
    }

    @Test
    void menuRemovalDeletesTheWholeSubtreeAndItsRoleBindings() {
        MenuDto root = menu("operations", "Operations", "/operations", MenuType.CATALOG, 0L, 10);
        menus.createMenu(root);
        MenuBo rootBo = menus.getMenuByPath("/operations");
        MenuDto child = menu("jobs", "Jobs", "/operations/jobs", MenuType.MENU, rootBo.getId(), 10);
        child.setComponent("/operations/jobs/index");
        menus.createMenu(child);
        MenuBo childBo = menus.getMenuByPath("/operations/jobs");
        MenuDto button = menu("jobs-run", "Run jobs", null, MenuType.BUTTON, childBo.getId(), 10);
        button.setAccessCode("jobs:run");
        menus.createMenu(button);
        Long buttonId = menus.getMenuByAccessCode("jobs:run").getId();
        roles.createRole(role("operator", "Operator"));
        Long roleId = roles.getRoleByCode("operator").getId();
        RoleMenuDto binding = new RoleMenuDto();
        binding.setRoleId(roleId);
        binding.setMenuIds(List.of(buttonId));
        roleMenus.saveRoleMenus(binding);
        jdbc.update("update az_menu set del = 1 where id = ?", childBo.getId());

        menus.removeMenu(rootBo.getId());

        assertThat(menus.getById(rootBo.getId())).isNull();
        assertThat(menus.getById(childBo.getId())).isNull();
        assertThat(menus.getById(buttonId)).isNull();
        assertThat(roleMenus.getMenuIds(roleId)).isEmpty();
        assertThat(jdbc.queryForObject("""
                select count(*) from az_menu
                where id in (?, ?, ?) and del = 1
                """, Integer.class, rootBo.getId(), childBo.getId(), buttonId)).isEqualTo(3);
    }

    @Test
    void menuRoutesKeepReachableEnabledNavigationAndMapVbenComponents() {
        MenuDto tools = menu("Tools", "Tools", "/tools", MenuType.CATALOG, 0L, 10);
        menus.createMenu(tools);
        Long toolsId = menus.getMenuByPath("/tools").getId();
        MenuDto audit = menu("Audit", "Audit", "audit", MenuType.MENU, toolsId, 1);
        audit.setComponent("/tools/audit/index");
        audit.setQuery("{\"tab\":\"recent\"}");
        menus.createMenu(audit);
        Long auditId = menus.getMenuByPath("audit").getId();
        MenuDto button = menu("AuditExport", "Export", null, MenuType.BUTTON, auditId, 1);
        button.setAccessCode("audit:export");
        menus.createMenu(button);
        Long buttonId = menus.getMenuByAccessCode("audit:export").getId();

        MenuDto embedded = menu("Documentation", "Documentation", "/docs", MenuType.EMBEDDED, 0L, 20);
        embedded.setIframeSrc("https://example.test/docs");
        menus.createMenu(embedded);
        Long embeddedId = menus.getMenuByPath("/docs").getId();
        MenuDto link = menu("Repository", "Repository", "/repository", MenuType.LINK, 0L, 30);
        link.setLink("https://example.test/repository");
        menus.createMenu(link);
        Long linkId = menus.getMenuByPath("/repository").getId();
        MenuDto empty = menu("Empty", "Empty", "/empty", MenuType.CATALOG, 0L, 40);
        menus.createMenu(empty);
        Long emptyId = menus.getMenuByPath("/empty").getId();

        MenuDto disabled = menu("Disabled", "Disabled", "/disabled", MenuType.CATALOG, 0L, 50);
        disabled.setStatus(false);
        menus.createMenu(disabled);
        Long disabledId = menus.getMenuByPath("/disabled").getId();
        MenuDto hiddenChild = menu("HiddenChild", "Hidden child", "child", MenuType.MENU, disabledId, 1);
        hiddenChild.setComponent("/disabled/child/index");
        menus.createMenu(hiddenChild);
        Long hiddenChildId = menus.getMenuByPath("child").getId();

        List<MenuRouteVo> routes = menus.getMenuRoutes(List.of(
                auditId, buttonId, embeddedId, linkId, emptyId, hiddenChildId, Long.MAX_VALUE));

        assertThat(routes).extracting(MenuRouteVo::getName)
                .containsExactly("Tools", "Documentation", "Repository");
        MenuRouteVo toolsRoute = routes.get(0);
        assertThat(toolsRoute.getComponent()).isNull();
        assertThat(toolsRoute.getChildren()).singleElement().satisfies(route -> {
            assertThat(route.getName()).isEqualTo("Audit");
            assertThat(route.getComponent()).isEqualTo("/tools/audit/index");
            assertThat(route.getMeta().getQuery()).containsEntry("tab", "recent");
            assertThat(route.getChildren()).isEmpty();
        });
        assertThat(routes.get(1).getComponent()).isEqualTo("IFrameView");
        assertThat(routes.get(2).getComponent()).isEqualTo("IFrameView");
    }

    @Test
    void linkMenusRequireAndPersistTheFrontendGeneratedPath() {
        MenuDto link = menu("Support", "Support", null, MenuType.LINK, 0L, 10);
        link.setLink("https://example.test/support");

        assertThatThrownBy(() -> menus.createMenu(link))
                .isInstanceOf(InvalidArgumentException.class);

        link.setPath("/support");
        menus.createMenu(link);

        assertThat(menus.getMenuByPath("/support"))
                .extracting(MenuBo::getLink)
                .isEqualTo("https://example.test/support");
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

    private long createMatrixParent(MenuType parentType) {
        if (parentType == null) {
            return 0L;
        }
        long parentPid = 0L;
        if (parentType == MenuType.BUTTON) {
            MenuDto container = validMenu("matrix-container", MenuType.CATALOG, 0L);
            menus.createMenu(container);
            parentPid = menus.getMenuByPath(container.getPath()).getId();
        }
        MenuDto parent = validMenu("matrix-parent", parentType, parentPid);
        menus.createMenu(parent);
        return findMenu(parent).getId();
    }

    private MenuDto validMenu(String name, MenuType type, long pid) {
        MenuDto dto = menu(
                name,
                name,
                type == MenuType.BUTTON ? null : '/' + name,
                type,
                pid,
                10);
        switch (type) {
            case MENU -> dto.setComponent("/views/" + name);
            case BUTTON -> dto.setAccessCode(name + ":action");
            case EMBEDDED -> dto.setIframeSrc("https://example.test/" + name);
            case LINK -> dto.setLink("https://example.test/" + name);
            case CATALOG -> {
            }
        }
        return dto;
    }

    private MenuBo findMenu(MenuDto dto) {
        return dto.getType() == MenuType.BUTTON
                ? menus.getMenuByAccessCode(dto.getAccessCode())
                : menus.getMenuByPath(dto.getPath());
    }

    private static Stream<Arguments> menuParentChildCases() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (MenuType childType : MenuType.values()) {
            cases.add(Arguments.of("root", null, childType, childType != MenuType.BUTTON));
        }
        for (MenuType parentType : MenuType.values()) {
            for (MenuType childType : MenuType.values()) {
                boolean accepted = parentType == MenuType.CATALOG
                        || (parentType == MenuType.MENU && childType == MenuType.BUTTON);
                cases.add(Arguments.of(parentType.name().toLowerCase(), parentType, childType, accepted));
            }
        }
        return cases.build();
    }

    private static Stream<Arguments> menuUrlBoundaries() {
        return Stream.of(
                Arguments.of(MenuType.EMBEDDED, 500, true),
                Arguments.of(MenuType.EMBEDDED, 501, false),
                Arguments.of(MenuType.LINK, 500, true),
                Arguments.of(MenuType.LINK, 501, false));
    }

    private static String httpUrl(int length) {
        String prefix = "https://example.test/";
        return prefix + "a".repeat(length - prefix.length());
    }

    private int activeRelationCount(String table, String ownerColumn, Long ownerId) {
        return jdbc.queryForObject(
                "select count(*) from " + table + " where " + ownerColumn + " = ? and del = 0",
                Integer.class,
                ownerId);
    }
}
