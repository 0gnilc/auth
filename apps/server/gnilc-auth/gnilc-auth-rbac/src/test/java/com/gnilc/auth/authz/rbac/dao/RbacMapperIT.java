package com.gnilc.auth.authz.rbac.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.support.RbacTestApplication;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = RbacTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RbacContainerContextInitializer.class)
@Transactional
class RbacMapperIT {
    @Autowired private RoleDao roles;
    @Autowired private PermissionDao permissions;
    @Autowired private MenuDao menus;
    @Autowired private UserDao users;
    @Autowired private UserRoleDao userRoles;
    @Autowired private RolePermissionDao rolePermissions;
    @Autowired private RoleMenusDao roleMenus;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void roleMappingUsesAutoFillUniqueIndexPaginationAndLogicalDelete() {
        RoleBo first = role("auditor", "Auditor");
        RoleBo second = role("operator", "Operator");
        roles.insert(first);
        roles.insert(second);

        assertThat(first.getId()).isNotNull();
        assertThat(first.getCreateTime()).isNotNull();
        IPage<RoleBo> page = roles.selectPage(Page.of(1, 1), null);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
        assertThatThrownBy(() -> roles.insert(role("auditor", "Duplicate")))
                .isInstanceOf(DuplicateKeyException.class);

        roles.deleteById(first.getId());

        assertThat(roles.selectById(first.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select del from az_role where id = ?", Integer.class, first.getId())).isEqualTo(1);
    }

    @Test
    void instantMappingUsesUtcSessionAndMicrosecondPrecision() {
        Instant instant = Instant.parse("2026-07-27T10:30:00.123456Z");
        RoleBo role = role("utc-auditor", "UTC Auditor");
        role.setCreateTime(instant);

        roles.insert(role);

        assertThat(jdbc.queryForObject("select @@session.time_zone", String.class))
                .isEqualTo("+00:00");
        assertThat(jdbc.queryForObject(
                "select date_format(create_time, '%Y-%m-%d %H:%i:%s.%f') from az_role where id = ?",
                String.class,
                role.getId()))
                .isEqualTo("2026-07-27 10:30:00.123456");
        assertThat(roles.selectById(role.getId()).getCreateTime())
                .isEqualTo(instant);
    }

    @Test
    void everyRbacMapperPersistsItsProductionTableMapping() {
        RoleBo role = role("manager", "Manager");
        roles.insert(role);
        PermissionBo permission = new PermissionBo();
        permission.setCode("report:read");
        permission.setName("Read reports");
        permission.setTargetIdentifier("/reports/**");
        permission.setTargetQualifier("GET");
        permission.setPublicAccess(false);
        permissions.insert(permission);
        MenuBo menu = menu("reports", "/reports", 0L, MenuType.MENU, 10);
        menus.insert(menu);
        UserBo user = new UserBo();
        users.insert(user);

        UserRoleBo userRole = new UserRoleBo();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoles.insert(userRole);
        RolePermissionBo rolePermission = new RolePermissionBo();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(permission.getId());
        rolePermissions.insert(rolePermission);
        RoleMenuBo roleMenu = new RoleMenuBo();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuId(menu.getId());
        roleMenus.insert(roleMenu);

        assertThat(permissions.selectById(permission.getId()).getTargetQualifier()).isEqualTo("GET");
        assertThat(menus.selectById(menu.getId()).getType()).isEqualTo(MenuType.MENU);
        assertThat(users.selectById(user.getId())).isNotNull();
        assertThat(userRoles.selectById(userRole.getId()).getRoleId()).isEqualTo(role.getId());
        assertThat(rolePermissions.selectById(rolePermission.getId()).getPermissionId())
                .isEqualTo(permission.getId());
        assertThat(roleMenus.selectById(roleMenu.getId()).getMenuId()).isEqualTo(menu.getId());
    }

    @Test
    void menuAndPermissionMappingsPersistBuiltInAndClearNullableMenuFields() {
        PermissionBo permission = new PermissionBo();
        permission.setCode("report:manage");
        permission.setName("Manage reports");
        permission.setTargetIdentifier("/reports/**");
        permission.setTargetQualifier("POST");
        permission.setPublicAccess(false);
        permission.setBuiltIn(true);
        permissions.insert(permission);

        MenuBo menu = menu("report-settings", "/reports/settings", 0L, MenuType.MENU, 20);
        menu.setBuiltIn(true);
        menu.setAccessCode("report:manage");
        menu.setRedirect("/reports");
        menu.setActivePath("/reports/settings");
        menu.setBadge("New");
        menu.setBadgeType("normal");
        menu.setBadgeVariants("primary");
        menu.setIcon("lucide:settings");
        menu.setIframeSrc("https://example.test/reports");
        menu.setLink("https://example.test");
        menu.setQuery("{\"tab\":\"settings\"}");
        menus.insert(menu);

        menu.setAccessCode(null);
        menu.setPath(null);
        menu.setComponent(null);
        menu.setRedirect(null);
        menu.setActivePath(null);
        menu.setBadge(null);
        menu.setBadgeType(null);
        menu.setBadgeVariants(null);
        menu.setIcon(null);
        menu.setIframeSrc(null);
        menu.setLink(null);
        menu.setQuery(null);
        menus.updateById(menu);

        assertThat(permissions.selectById(permission.getId()).getBuiltIn()).isTrue();
        assertThat(menus.selectById(menu.getId()))
                .returns(true, MenuBo::getBuiltIn)
                .returns(null, MenuBo::getAccessCode)
                .returns(null, MenuBo::getPath)
                .returns(null, MenuBo::getComponent)
                .returns(null, MenuBo::getRedirect)
                .returns(null, MenuBo::getActivePath)
                .returns(null, MenuBo::getBadge)
                .returns(null, MenuBo::getBadgeType)
                .returns(null, MenuBo::getBadgeVariants)
                .returns(null, MenuBo::getIcon)
                .returns(null, MenuBo::getIframeSrc)
                .returns(null, MenuBo::getLink)
                .returns(null, MenuBo::getQuery);
    }

    private RoleBo role(String code, String name) {
        RoleBo role = new RoleBo();
        role.setCode(code);
        role.setName(name);
        role.setBuiltIn(false);
        return role;
    }

    private MenuBo menu(String name, String path, long pid, MenuType type, int order) {
        MenuBo menu = new MenuBo();
        menu.setPid(pid);
        menu.setType(type);
        menu.setStatus(true);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent("views/" + name);
        menu.setOrder(order);
        menu.setTitle(name);
        return menu;
    }
}
