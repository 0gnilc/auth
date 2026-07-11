package com.gnilc.auth.authz.rbac.provider.cache;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.auth.authz.rbac.config.MybatisPlusConfiguration;
import com.gnilc.auth.authz.rbac.dao.PermissionDao;
import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.dao.RolePermissionDao;
import com.gnilc.auth.authz.rbac.dao.UserDao;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.impl.PermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.RolePermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserRoleServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserServiceImpl;
import com.gnilc.test.annotation.IntegrationTest;
import com.gnilc.auth.authz.rbac.support.RbacMySqlContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        classes = DatabasePermissionCacheLoaderIT.LoaderConfiguration.class,
        initializers = RbacMySqlContainerContextInitializer.class
)
class DatabasePermissionCacheLoaderIT {
    @Autowired
    private DatabasePermissionCacheLoader loader;
    @Autowired
    private UserDao userDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PermissionDao permissionDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private RolePermissionDao rolePermissionDao;

    @Test
    void loadsTargetPublicAndUserPermissionViewsFromMysql() {
        UserBo user = new UserBo();
        userDao.insert(user);
        RoleBo role = role("case-worker");
        roleDao.insert(role);
        PermissionBo assigned = permission("case:read", "/cases/**", false);
        PermissionBo publicPermission = permission("status:read", "/status", true);
        permissionDao.insert(assigned);
        permissionDao.insert(publicPermission);

        UserRoleBo userRole = new UserRoleBo();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleDao.insert(userRole);
        RolePermissionBo rolePermission = new RolePermissionBo();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(assigned.getId());
        rolePermissionDao.insert(rolePermission);

        assertThat(loader.loadTargetPermissions()).containsExactlyInAnyOrder(
                new TargetPermission("/cases/**", "case:read"),
                new TargetPermission("/status", "status:read")
        );
        assertThat(loader.loadPublicAccessPermissions())
                .containsExactly(new Permission("status:read"));
        assertThat(loader.loadUserPermissions(user.getId()))
                .containsExactly(new Permission("case:read"));
        assertThat(loader.loadUserPermissions(Long.MAX_VALUE)).isEmpty();
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

    @Configuration(proxyBeanMethods = false)
    @MapperScan("com.gnilc.auth.authz.rbac.dao")
    @Import({
            MybatisPlusConfiguration.class,
            MyMetaObjectHandler.class,
            DatabasePermissionCacheLoader.class,
            PermissionServiceImpl.class,
            RolePermissionServiceImpl.class,
            UserRoleServiceImpl.class,
            UserServiceImpl.class
    })
    static class LoaderConfiguration {
        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean
        MenuService menuService() {
            return mock(MenuService.class);
        }
    }
}
