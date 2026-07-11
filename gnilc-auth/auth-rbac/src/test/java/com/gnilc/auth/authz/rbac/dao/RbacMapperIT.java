package com.gnilc.auth.authz.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.auth.authz.rbac.config.MybatisPlusConfiguration;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.auth.authz.rbac.support.RbacMySqlContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@MybatisPlusTest(properties = {
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=del",
        "mybatis-plus.global-config.db-config.logic-delete-value=true",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=false",
        "mybatis-plus.global-config.db-config.id-type=auto"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
        classes = RbacMapperIT.MapperConfiguration.class,
        initializers = RbacMySqlContainerContextInitializer.class
)
class RbacMapperIT {
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PermissionDao permissionDao;
    @Autowired
    private MenuDao menuDao;
    @Autowired
    private RolePermissionDao rolePermissionDao;
    @Autowired
    private RoleMenusDao roleMenusDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsRepresentativeRolePermissionAndMenuMappings() {
        RoleBo role = role("support-agent");
        PermissionBo permission = permission("ticket:read", "/tickets/**", false);
        MenuBo menu = menu("ticket-list", "/tickets", "ticket:read");
        roleDao.insert(role);
        permissionDao.insert(permission);
        menuDao.insert(menu);

        RolePermissionBo rolePermission = new RolePermissionBo();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(permission.getId());
        rolePermissionDao.insert(rolePermission);
        RoleMenuBo roleMenu = new RoleMenuBo();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuId(menu.getId());
        roleMenusDao.insert(roleMenu);

        assertThat(rolePermissionDao.selectList(new LambdaQueryWrapper<RolePermissionBo>()
                .eq(RolePermissionBo::getRoleId, role.getId())))
                .extracting(RolePermissionBo::getPermissionId)
                .containsExactly(permission.getId());
        assertThat(roleMenusDao.selectList(new LambdaQueryWrapper<RoleMenuBo>()
                .eq(RoleMenuBo::getRoleId, role.getId())))
                .extracting(RoleMenuBo::getMenuId)
                .containsExactly(menu.getId());
    }

    @Test
    void enforcesUniqueCodesAndHidesLogicallyDeletedRows() {
        RoleBo role = role("auditor");
        roleDao.insert(role);

        assertThatThrownBy(() -> roleDao.insert(role("auditor")))
                .isInstanceOf(DataIntegrityViolationException.class);

        roleDao.deleteById(role.getId());

        assertThat(roleDao.selectById(role.getId())).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select del from az_role where id = ?", Integer.class, role.getId()))
                .isEqualTo(1);
    }

    @Test
    void mapsMysqlJsonAndPaginatesWithStableOrdering() {
        MenuBo menu = menu("case-list", "/cases", "case:read");
        menu.setQuery("{\"status\":\"open\",\"limit\":20}");
        menuDao.insert(menu);

        assertThat(menuDao.selectById(menu.getId()).getQuery())
                .isEqualTo("{\"limit\": 20, \"status\": \"open\"}");

        roleDao.insert(role("page-role-a"));
        roleDao.insert(role("page-role-b"));
        roleDao.insert(role("page-role-c"));
        Page<RoleBo> page = roleDao.selectPage(
                Page.of(2, 2),
                new LambdaQueryWrapper<RoleBo>()
                        .likeRight(RoleBo::getCode, "page-role-")
                        .orderByAsc(RoleBo::getCode));

        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getPages()).isEqualTo(2);
        assertThat(page.getRecords())
                .extracting(RoleBo::getCode)
                .containsExactly("page-role-c");
    }

    @Test
    void schemaProvidesIndexesForPermissionAndRelationLookups() {
        assertThat(indexColumns("az_permission", "idx_target_identifier"))
                .containsExactly("target_identifier");
        assertThat(indexColumns("az_user_role", "idx_user_role"))
                .containsExactly("user_id", "role_id");
        assertThat(indexColumns("az_role_permission", "idx_role_permission"))
                .containsExactly("role_id", "permission_id");
        assertThat(indexColumns("az_role_menu", "idx_role_menu"))
                .containsExactly("role_id", "menu_id");
    }

    private java.util.List<String> indexColumns(String table, String index) {
        return jdbcTemplate.queryForList("""
                        select column_name
                        from information_schema.statistics
                        where table_schema = database() and table_name = ? and index_name = ?
                        order by seq_in_index
                        """, String.class, table, index);
    }

    private RoleBo role(String code) {
        RoleBo role = new RoleBo();
        role.setCode(code);
        role.setName(code);
        role.setBuiltIn(false);
        return role;
    }

    private PermissionBo permission(String code, String target, boolean publicAccess) {
        PermissionBo permission = new PermissionBo();
        permission.setCode(code);
        permission.setName(code);
        permission.setTargetIdentifier(target);
        permission.setPublicAccess(publicAccess);
        return permission;
    }

    private MenuBo menu(String name, String path, String accessCode) {
        MenuBo menu = new MenuBo();
        menu.setPid(0L);
        menu.setType(MenuType.MENU);
        menu.setStatus(true);
        menu.setAccessCode(accessCode);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent("views/tickets/index");
        menu.setOrder(10);
        menu.setTitle("Tickets");
        return menu;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("com.gnilc.auth.authz.rbac.dao")
    @Import({MybatisPlusConfiguration.class, MyMetaObjectHandler.class})
    static class MapperConfiguration {
    }
}
