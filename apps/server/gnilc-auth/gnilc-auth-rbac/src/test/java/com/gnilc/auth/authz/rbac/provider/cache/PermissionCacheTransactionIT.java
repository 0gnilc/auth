package com.gnilc.auth.authz.rbac.provider.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.gnilc.auth.authz.rbac.dao.RolePermissionDao;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.provider.cache.redis.PermissionCacheRedisResetTransport;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.impl.RolePermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserRoleServiceImpl;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import com.gnilc.common.config.MyMetaObjectHandler;
import com.gnilc.common.config.MybatisPlusConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(
        classes = PermissionCacheTransactionIT.TransactionConfiguration.class,
        initializers = RbacContainerContextInitializer.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PermissionCacheTransactionIT {
    @Autowired private RolePermissionServiceImpl rolePermissionService;
    @Autowired private UserRoleServiceImpl userRoleService;
    @Autowired private RolePermissionDao rolePermissionDao;
    @Autowired private UserRoleDao userRoleDao;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PermissionCacheService cacheService;
    @Autowired private PermissionCacheRedisResetTransport redisResetTransport;

    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        cleanCommittedRelations();
        clearInvocations(cacheService, redisResetTransport);
    }

    @AfterEach
    void cleanCommittedRelations() {
        jdbc.update("delete from az_role_permission where role_id between 7101 and 7102");
        jdbc.update("delete from az_user_role where role_id between 7101 and 7102");
    }

    @Test
    void committedRelationChangeInvalidatesAffectedUserAfterCommit() {
        long roleId = 7101L;
        long userId = 7201L;
        seedUserRole(userId, roleId);
        seedRolePermission(roleId, 7301L);

        transactions.executeWithoutResult(status -> {
            rolePermissionService.saveRolePermissions(replacement(roleId, 7302L, 7303L, 7303L));

            assertThat(permissionIds(roleId)).containsExactlyInAnyOrder(7302L, 7303L);
            verifyNoInteractions(cacheService, redisResetTransport);
        });

        PermissionCacheResetCommand expected = PermissionCacheResetCommand.userPermissions(userId);
        verify(cacheService).resetUserPermissions(userId);
        verify(redisResetTransport).publish(expected);
    }

    @Test
    void rolledBackRelationChangePreservesDataAndDoesNotInvalidateCache() {
        long roleId = 7102L;
        long userId = 7202L;
        seedUserRole(userId, roleId);
        seedRolePermission(roleId, 7304L);

        transactions.executeWithoutResult(status -> {
            rolePermissionService.saveRolePermissions(replacement(roleId, 7305L));
            assertThat(permissionIds(roleId)).containsExactly(7305L);
            verifyNoInteractions(cacheService, redisResetTransport);
            status.setRollbackOnly();
        });

        assertThat(permissionIds(roleId)).containsExactly(7304L);
        verifyNoInteractions(cacheService, redisResetTransport);
    }

    @Test
    void committedPermissionRemovalInvalidatesAffectedUserAfterCommit() {
        long roleId = 7101L;
        long userId = 7201L;
        seedUserRole(userId, roleId);
        seedRolePermission(roleId, 7306L);

        transactions.executeWithoutResult(status -> {
            rolePermissionService.removeByPermissionId(7306L);

            assertThat(permissionIds(roleId)).isEmpty();
            verifyNoInteractions(cacheService, redisResetTransport);
        });

        PermissionCacheResetCommand expected = PermissionCacheResetCommand.userPermissions(userId);
        verify(cacheService).resetUserPermissions(userId);
        verify(redisResetTransport).publish(expected);
    }

    @Test
    void committedRoleRemovalInvalidatesAffectedUserAfterCommit() {
        long roleId = 7101L;
        long userId = 7201L;
        seedUserRole(userId, roleId);

        transactions.executeWithoutResult(status -> {
            userRoleService.removeByRoleId(roleId);

            assertThat(userRoleDao.selectList(new LambdaQueryWrapper<UserRoleBo>()
                    .eq(UserRoleBo::getRoleId, roleId))).isEmpty();
            verifyNoInteractions(cacheService, redisResetTransport);
        });

        PermissionCacheResetCommand expected = PermissionCacheResetCommand.userPermissions(userId);
        verify(cacheService).resetUserPermissions(userId);
        verify(redisResetTransport).publish(expected);
    }

    private void seedUserRole(long userId, long roleId) {
        transactions.executeWithoutResult(status -> {
            UserRoleBo relation = new UserRoleBo();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleDao.insert(relation);
        });
    }

    private void seedRolePermission(long roleId, long permissionId) {
        transactions.executeWithoutResult(status -> {
            RolePermissionBo relation = new RolePermissionBo();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionDao.insert(relation);
        });
    }

    private List<Long> permissionIds(long roleId) {
        return rolePermissionDao.selectList(new LambdaQueryWrapper<RolePermissionBo>()
                        .select(RolePermissionBo::getPermissionId)
                        .eq(RolePermissionBo::getRoleId, roleId))
                .stream()
                .map(RolePermissionBo::getPermissionId)
                .toList();
    }

    private RolePermissionDto replacement(long roleId, Long... permissionIds) {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleId(roleId);
        dto.setPermissionIds(List.of(permissionIds));
        return dto;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("com.gnilc.auth.authz.rbac.dao")
    @Import({
            MybatisPlusConfiguration.class,
            MyMetaObjectHandler.class,
            RolePermissionServiceImpl.class,
            UserRoleServiceImpl.class,
            PermissionCacheResetPolicy.class,
            PermissionCacheResetExecutor.class,
            PermissionCacheResetEventListener.class
    })
    static class TransactionConfiguration {
        @Bean
        PermissionCacheService permissionCacheService() {
            return mock(PermissionCacheService.class);
        }

        @Bean
        PermissionCacheRedisResetTransport redisResetTransport() {
            return mock(PermissionCacheRedisResetTransport.class);
        }

        @Bean
        RoleService roleService() {
            RoleService roleService = mock(RoleService.class);
            RoleBo role = new RoleBo();
            role.setBuiltIn(Boolean.FALSE);
            when(roleService.getById(7101L)).thenReturn(role);
            when(roleService.getById(7102L)).thenReturn(role);
            return roleService;
        }

        @Bean
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);
            when(permissionService.getPermissions(anyList())).thenAnswer(invocation -> {
                List<Long> ids = invocation.getArgument(0);
                return ids.stream().map(id -> {
                    PermissionBo permission = new PermissionBo();
                    permission.setId(id);
                    return permission;
                }).toList();
            });
            return permissionService;
        }

        @Bean
        I18nMessageService i18nMessageService() {
            return mock(I18nMessageService.class);
        }
    }
}
