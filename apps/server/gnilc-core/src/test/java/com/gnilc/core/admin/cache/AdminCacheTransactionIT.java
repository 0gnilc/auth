package com.gnilc.core.admin.cache;

import com.gnilc.core.admin.entity.dto.AdminDto;
import com.gnilc.core.admin.service.AdminService;
import com.gnilc.core.context.UserContextService;
import com.gnilc.core.support.SystemContainerContextInitializer;
import com.gnilc.core.support.SystemTestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 通过显式事务验证首次缓存删除失败会回滚资料更新，主动回滚不会触发提交前监听器。 */
@SpringBootTest(classes = {
        SystemTestApplication.class,
        AdminCacheTransactionIT.CacheFailureConfiguration.class
})
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminCacheTransactionIT {
    private static final long USER_ID = 98_001L;

    @Autowired
    private AdminService admins;
    @Autowired
    private AdminCacheService cache;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private UserContextService userContextService;

    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        reset(cache);
        deleteAdmin();
        jdbc.update("""
                insert into sys_admin
                    (del, create_time, user_id, username, password, nickname, home_path, status)
                values (0, UTC_TIMESTAMP(6), ?, 'cache-transaction', 'hash', 'Before', '/dashboard', 1)
                """, USER_ID);
        authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        deleteAdmin();
        reset(cache);
    }

    /** 在真实提交阶段让首次 Redis 删除抛错，必须观察到数据库旧值和没有安排二次删除。 */
    @Test
    void firstCacheDeleteFailureRollsBackTheDatabaseUpdate() {
        doThrow(new IllegalStateException("redis deletion failed"))
                .when(cache).removeUserInfo(USER_ID);
        AdminDto profile = profile("After");

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> admins.updateProfile(profile)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis deletion failed");

        assertThat(nickname()).isEqualTo("Before");
        verify(cache).removeUserInfo(USER_ID);
        verify(cache, never()).scheduleSecondDelete(org.mockito.ArgumentMatchers.any());
    }

    /** 显式标记事务回滚，使 BEFORE_COMMIT 不被触发；缓存替身应完全没有调用。 */
    @Test
    void rolledBackTransactionDoesNotDeleteOrScheduleCacheEntries() {
        transactions.executeWithoutResult(status -> {
            admins.updateProfile(profile("After"));
            status.setRollbackOnly();
        });

        assertThat(nickname()).isEqualTo("Before");
        verifyNoInteractions(cache);
    }

    private AdminDto profile(String nickname) {
        AdminDto profile = new AdminDto();
        profile.setNickname(nickname);
        return profile;
    }

    private String nickname() {
        return jdbc.queryForObject(
                "select nickname from sys_admin where user_id = ? and del = 0",
                String.class,
                USER_ID);
    }

    private void authenticate(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(com.gnilc.auth.authn.context.DefaultAccessPrincipal.of(userId));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThat(userContextService.getUserId()).isEqualTo(userId);
    }

    private void deleteAdmin() {
        jdbc.update("delete from sys_admin where user_id = ?", USER_ID);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheFailureConfiguration {
        @Bean
        @Primary
        AdminCacheService failingAdminCacheService() {
            return mock(AdminCacheService.class);
        }
    }
}
