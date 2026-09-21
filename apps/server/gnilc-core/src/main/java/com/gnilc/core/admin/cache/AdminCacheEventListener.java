package com.gnilc.core.admin.cache;

import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.event.MenuEvent;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.core.admin.event.AdminEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 将管理员与 RBAC 数据变化事件映射为管理员查询缓存删除动作。
 */
@Component
public class AdminCacheEventListener {
    private final AdminCacheService adminCacheService;
    private final UserRoleService userRoleService;

    public AdminCacheEventListener(AdminCacheService adminCacheService, UserRoleService userRoleService) {
        this.adminCacheService = adminCacheService;
        this.userRoleService = userRoleService;
    }

    /** 在提交前使管理员资料缓存失效；无事务发布事件时立即执行。 */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAdmin(AdminEvent event) {
        if (event != null) {
            reset(() -> adminCacheService.removeUserInfo(event.getUserId()));
        }
    }

    /** 菜单树变化影响各管理员的导航和按钮可见性，统一删除这两类查询缓存。 */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleMenu(MenuEvent event) {
        if (event != null) {
            reset(() -> {
                adminCacheService.removeAllMenuAccessCodes();
                adminCacheService.removeAllMenuRoutes();
            });
        }
    }

    /** 按角色或用户定位受影响查询缓存，避免将权限决策缓存与管理员导航缓存混为一类。 */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAuthorization(AuthorizationEvent<Long> event) {
        if (event == null || event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case ROLE -> resetRoleCodes(usersForRole(event.getData()));
            case ROLE_MENU -> resetMenuAccessCodesAndRoutes(usersForRole(event.getData()));
            case USER, USER_ROLE -> resetUserAuthorization(event.getData());
            case PERMISSION, ROLE_PERMISSION, ALL -> {
                // 权限本身不改变管理员资料、角色编码或导航查询；全量事件由无载荷入口处理。
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAllAuthorization(AuthorizationEvent<Void> event) {
        if (event != null && event.getType() == AuthorizationEvent.Type.ALL) {
            reset(() -> {
                adminCacheService.removeAllRoleCodes();
                adminCacheService.removeAllMenuAccessCodes();
                adminCacheService.removeAllMenuRoutes();
            });
        }
    }

    private void resetRoleCodes(List<Long> userIds) {
        if (!CollectionUtils.isEmpty(userIds)) {
            List<Long> affectedUserIds = userIds.stream().distinct().toList();
            reset(() -> affectedUserIds.forEach(adminCacheService::removeRoleCodes));
        }
    }

    private void resetMenuAccessCodesAndRoutes(List<Long> userIds) {
        if (!CollectionUtils.isEmpty(userIds)) {
            List<Long> affectedUserIds = userIds.stream().distinct().toList();
            reset(() -> affectedUserIds.forEach(userId -> {
                adminCacheService.removeMenuAccessCodes(userId);
                adminCacheService.removeMenuRoutes(userId);
            }));
        }
    }

    private void resetUserAuthorization(Long userId) {
        if (userId != null) {
            reset(() -> {
                adminCacheService.removeRoleCodes(userId);
                adminCacheService.removeMenuAccessCodes(userId);
                adminCacheService.removeMenuRoutes(userId);
            });
        }
    }

    private List<Long> usersForRole(Long roleId) {
        return roleId == null ? List.of() : userRoleService.getUserIds(roleId);
    }

    /** 首次删除在当前调用内完成，失败阻止事务提交；成功提交后再安排延迟删除，回滚不安排。 */
    private void reset(Runnable resetAction) {
        // 先删后提交保留 Redis 故障可回滚的机会；二次删除处理并发读取在提交前回填旧值的窗口。
        resetAction.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    adminCacheService.scheduleSecondDelete(resetAction);
                }
            });
            return;
        }
        adminCacheService.scheduleSecondDelete(resetAction);
    }
}
