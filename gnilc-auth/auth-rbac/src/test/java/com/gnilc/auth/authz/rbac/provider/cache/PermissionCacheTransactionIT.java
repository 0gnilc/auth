package com.gnilc.auth.authz.rbac.provider.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.gnilc.auth.authz.rbac.config.MyMetaObjectHandler;
import com.gnilc.auth.authz.rbac.config.MybatisPlusConfiguration;
import com.gnilc.auth.authz.rbac.dao.RolePermissionDao;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.provider.cache.redis.PermissionCacheRedisResetTransport;
import com.gnilc.auth.authz.rbac.service.impl.RolePermissionServiceImpl;
import com.gnilc.auth.authz.rbac.service.impl.UserRoleServiceImpl;
import com.gnilc.auth.authz.rbac.support.RbacContainerContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(
        classes = PermissionCacheTransactionIT.TransactionConfiguration.class,
        initializers = RbacContainerContextInitializer.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PermissionCacheTransactionIT {
    @Autowired private RolePermissionServiceImpl rolePermissionService;
    @Autowired private RolePermissionDao rolePermissionDao;
    @Autowired private UserRoleDao userRoleDao;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PermissionCache permissionCache;
    @Autowired private PermissionCacheRedisResetTransport redisResetTransport;

    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        clearInvocations(permissionCache, redisResetTransport);
    }

    @AfterEach
    void cleanCommittedRelations() {
        transactions.executeWithoutResult(status -> {
            rolePermissionDao.delete(new LambdaQueryWrapper<RolePermissionBo>()
                    .between(RolePermissionBo::getRoleId, 7101L, 7102L));
            userRoleDao.delete(new LambdaQueryWrapper<UserRoleBo>()
                    .between(UserRoleBo::getRoleId, 7101L, 7102L));
        });
    }

    @Test
    void committedRelationChangeInvalidatesAffectedUserAfterCommit() {
        long roleId = 7101L;
        long userId = 7201L;
        seedUserRole(userId, roleId);
        seedRolePermission(roleId, 7301L);

        transactions.executeWithoutResult(status -> {
            rolePermissionService.updateRolePermission(replacement(roleId, 7302L, 7303L, 7303L));

            assertThat(permissionIds(roleId)).containsExactlyInAnyOrder(7302L, 7303L);
            verifyNoInteractions(permissionCache, redisResetTransport);
        });

        PermissionCacheResetCommand expected = PermissionCacheResetCommand.userPermissions(userId);
        verify(permissionCache).resetUserPermissions(userId);
        verify(redisResetTransport).publish(expected);
    }

    @Test
    void rolledBackRelationChangePreservesDataAndDoesNotInvalidateCache() {
        long roleId = 7102L;
        long userId = 7202L;
        seedUserRole(userId, roleId);
        seedRolePermission(roleId, 7304L);

        transactions.executeWithoutResult(status -> {
            rolePermissionService.updateRolePermission(replacement(roleId, 7305L));
            assertThat(permissionIds(roleId)).containsExactly(7305L);
            verifyNoInteractions(permissionCache, redisResetTransport);
            status.setRollbackOnly();
        });

        assertThat(permissionIds(roleId)).containsExactly(7304L);
        verifyNoInteractions(permissionCache, redisResetTransport);
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
        PermissionCache permissionCache() {
            return mock(PermissionCache.class);
        }

        @Bean
        PermissionCacheRedisResetTransport redisResetTransport() {
            return mock(PermissionCacheRedisResetTransport.class);
        }
    }
}
