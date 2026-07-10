package com.gnilc.auth.authz.rbac.service;

import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.auth.authz.rbac.config.MybatisPlusConfiguration;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.service.impl.MenuServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.PermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.RoleMenuServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.RolePermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.RoleServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserRoleServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserServiceImpl;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.test.container.MySqlContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@SpringBootTest(
        classes = RbacServiceIT.ServiceTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        }
)
@ContextConfiguration(initializers = MySqlContainerContextInitializer.class)
@Transactional
class RbacServiceIT {
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private RoleMenuService roleMenuService;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;

    @Test
    void permissionAndRoleLifecyclesPreserveQueryAndReuseContracts() {
        createPermission("case:read", "Read cases", "/cases/**", false);
        PermissionBo permission = permissionService.getPermissionByCode("case:read");
        PermissionQueryDto permissionQuery = new PermissionQueryDto();
        permissionQuery.setTargetIdentifier("cases");
        permissionQuery.setPublicAccess(false);

        assertThat(permissionService.getPermissions(permissionQuery))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(permission.getId());
                    assertThat(found.getName()).isEqualTo("Read cases");
                });
        assertThatThrownBy(() -> createPermission("case:read", "Duplicate", "/duplicate", false))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("权限标识已存在");

        PermissionDto permissionUpdate = permission("case:view", "View cases", "/cases/**", true);
        permissionUpdate.setId(permission.getId());
        permissionService.updatePermission(permissionUpdate);
        assertThat(permissionService.getPermissionByCode("case:read")).isNull();
        assertThat(permissionService.getPermissionByCode("case:view").getPublicAccess()).isTrue();
        permissionService.removePermission(permission.getId());
        assertThat(permissionService.getPermissionByCode("case:view")).isNull();
        createPermission("case:view", "Reusable permission code", "/cases/view", false);

        createRole("auditor", "Auditor");
        RoleBo role = roleService.getRoleByCode("auditor");
        RoleQueryDto roleQuery = new RoleQueryDto();
        roleQuery.setName("Audit");
        roleQuery.setBuiltIn(false);
        assertThat(roleService.getRoles(roleQuery))
                .extracting("code")
                .containsExactly("auditor");
        RolePageDto pageQuery = new RolePageDto();
        pageQuery.setCode("auditor");
        pageQuery.setCurrentPage(1L);
        pageQuery.setPageSize(5L);
        assertThat(roleService.getRolePage(pageQuery).getList())
                .extracting("id")
                .containsExactly(role.getId());

        RoleDto roleUpdate = role("reviewer", "Reviewer");
        roleUpdate.setId(role.getId());
        roleService.updateRole(roleUpdate);
        assertThat(roleService.getRoleByCode("auditor")).isNull();
        roleService.removeRole(role.getId());
        assertThat(roleService.getRoleByCode("reviewer")).isNull();
        createRole("reviewer", "Reusable role code");
    }

    @Test
    void menuLifecycleBuildsAndSortsAValidatedTree() {
        menuService.createMenu(menu(0L, MenuType.CATALOG, "workspace", "/workspace", null, null, 20));
        Long rootId = menuService.getMenuByPath("/workspace").getId();
        menuService.createMenu(menu(rootId, MenuType.MENU, "accounts", "/workspace/accounts",
                "views/accounts/index", null, 2));
        menuService.createMenu(menu(rootId, MenuType.BUTTON, "account-create", null,
                null, "account:create", 1));

        assertThat(menuService.getMenuTree()).singleElement().satisfies(root -> {
            assertThat(root.getName()).isEqualTo("workspace");
            assertThat(root.getChildren())
                    .extracting("name")
                    .containsExactly("account-create", "accounts");
        });
        assertThat(menuService.getMenuByPath("/workspace/accounts").getComponent())
                .isEqualTo("views/accounts/index");
        MenuBo button = menuService.getMenuByAccessCode("account:create");
        MenuDto update = new MenuDto();
        update.setId(button.getId());
        update.setTitle("Create account");
        menuService.updateMenu(update);
        assertThat(menuService.getById(button.getId()).getTitle()).isEqualTo("Create account");
        menuService.removeMenu(button.getId());
        assertThat(menuService.getMenuByAccessCode("account:create")).isNull();

        MenuDto invalid = menu(0L, MenuType.MENU, "missing-component", "/missing", null, null, 3);
        assertThatThrownBy(() -> menuService.createMenu(invalid))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入页面组件");
    }

    @Test
    void relationServicesReplaceSetsAndDriveTheUserFacade() {
        createRole("operator", "Operator");
        createRole("auditor", "Auditor");
        RoleBo operator = roleService.getRoleByCode("operator");
        RoleBo auditor = roleService.getRoleByCode("auditor");
        createPermission("ticket:read", "Read tickets", "/tickets/**", false);
        createPermission("ticket:write", "Write tickets", "/tickets/**", false);
        PermissionBo read = permissionService.getPermissionByCode("ticket:read");
        PermissionBo write = permissionService.getPermissionByCode("ticket:write");
        menuService.createMenu(menu(0L, MenuType.BUTTON, "ticket-read", null, null, "ticket:read", 1));
        menuService.createMenu(menu(0L, MenuType.BUTTON, "ticket-write", null, null, "ticket:write", 2));
        MenuBo readMenu = menuService.getMenuByAccessCode("ticket:read");
        MenuBo writeMenu = menuService.getMenuByAccessCode("ticket:write");
        Long userId = userService.createUser();

        assertThat(userService.bindRole(userId, "operator")).isTrue();
        assertThat(userService.bindRole(userId, "missing")).isFalse();
        rolePermissionService.updateRolePermission(rolePermissions(operator.getId(), read.getId(), write.getId()));
        roleMenuService.updateRoleMenu(roleMenus(operator.getId(), readMenu.getId(), writeMenu.getId()));

        assertThat(userService.checkRole(userId, "operator")).isTrue();
        assertThat(userService.getRoles(userId)).extracting(RoleBo::getCode).containsExactly("operator");
        assertThat(userService.getPermissions(userId)).extracting(PermissionBo::getCode)
                .containsExactlyInAnyOrder("ticket:read", "ticket:write");
        assertThat(userService.getMenus(userId)).extracting(MenuBo::getAccessCode)
                .containsExactly("ticket:read", "ticket:write");
        assertThat(rolePermissionService.getRoleIds(write.getId())).containsExactly(operator.getId());

        rolePermissionService.updateRolePermission(rolePermissions(operator.getId(), write.getId()));
        roleMenuService.updateRoleMenu(roleMenus(operator.getId(), writeMenu.getId()));
        UserRoleDto replacement = new UserRoleDto();
        replacement.setUserId(userId);
        replacement.setRoleIds(List.of(operator.getId(), auditor.getId(), auditor.getId()));
        userRoleService.updateUserRole(replacement);

        assertThat(rolePermissionService.getPermissionIds(operator.getId())).containsExactly(write.getId());
        assertThat(roleMenuService.getMenuIds(operator.getId())).containsExactly(writeMenu.getId());
        assertThat(userRoleService.getRoleIds(userId))
                .containsExactlyInAnyOrder(operator.getId(), auditor.getId());
        assertThat(userRoleService.getUserIds(List.of(operator.getId(), auditor.getId())))
                .containsExactly(userId);

        assertThat(userService.unbindRole(userId, "operator")).isTrue();
        assertThat(userService.checkRole(userId, "operator")).isFalse();
        assertThat(userService.removeUser(userId)).isTrue();
        assertThat(userService.geUser(userId)).isNull();
    }

    @Test
    void serviceValidationRejectsMissingPublicInputsBeforeWriting() {
        assertThatThrownBy(() -> permissionService.createPermission(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写权限信息");
        assertThatThrownBy(() -> roleService.createRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写角色信息");
        assertThatThrownBy(() -> menuService.createMenu(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写菜单信息");
        assertThatThrownBy(() -> rolePermissionService.updateRolePermission(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写角色权限信息");
        assertThatThrownBy(() -> roleMenuService.updateRoleMenu(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写角色菜单信息");
        assertThatThrownBy(() -> userRoleService.updateUserRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请填写用户角色信息");
    }

    private void createPermission(String code, String name, String target, boolean publicAccess) {
        permissionService.createPermission(permission(code, name, target, publicAccess));
    }

    private PermissionDto permission(String code, String name, String target, boolean publicAccess) {
        PermissionDto dto = new PermissionDto();
        dto.setCode(code);
        dto.setName(name);
        dto.setTargetIdentifier(target);
        dto.setTargetQualifier("GET");
        dto.setPublicAccess(publicAccess);
        return dto;
    }

    private void createRole(String code, String name) {
        roleService.createRole(role(code, name));
    }

    private RoleDto role(String code, String name) {
        RoleDto dto = new RoleDto();
        dto.setCode(code);
        dto.setName(name);
        return dto;
    }

    private MenuDto menu(Long parentId, MenuType type, String name, String path,
                         String component, String accessCode, int order) {
        MenuDto dto = new MenuDto();
        dto.setPid(parentId);
        dto.setType(type);
        dto.setStatus(true);
        dto.setName(name);
        dto.setTitle(name);
        dto.setPath(path);
        dto.setComponent(component);
        dto.setAccessCode(accessCode);
        dto.setOrder(order);
        return dto;
    }

    private RolePermissionDto rolePermissions(Long roleId, Long... permissionIds) {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleId(roleId);
        dto.setPermissionIds(List.of(permissionIds));
        return dto;
    }

    private RoleMenuDto roleMenus(Long roleId, Long... menuIds) {
        RoleMenuDto dto = new RoleMenuDto();
        dto.setRoleId(roleId);
        dto.setMenuIds(List.of(menuIds));
        return dto;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            PermissionServiceImpl.class,
            RoleServiceImpl.class,
            MenuServiceImpl.class,
            RoleMenuServiceImpl.class,
            RolePermissionServiceImpl.class,
            UserRoleServiceImpl.class,
            UserServiceImpl.class,
            MybatisPlusConfiguration.class,
            MyMetaObjectHandler.class
    })
    @MapperScan(basePackageClasses = MenuDao.class)
    static class ServiceTestApplication {
    }
}
