package com.gnilc.auth.authz.decision;

import com.google.common.base.Preconditions;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 默认访问决策器基类。
 * <p>
 * 该策略要求已授予权限覆盖所有所需权限；子类可覆盖 {@link #decide(AccessContext)} 提供不同策略。
 */
public class AbstractAccessDecision implements AccessDecision {
    /**
     * 已授予权限提供者。
     */
    protected final GrantedPermissionsProvider grantedPermissionsProvider;
    /**
     * 所需权限提供者。
     */
    protected final RequiredPermissionsProvider requiredPermissionsProvider;

    /**
     * 创建访问决策器。
     *
     * @param grantedPermissionsProvider  已授予权限提供者
     * @param requiredPermissionsProvider 所需权限提供者
     * @throws IllegalArgumentException provider 为空时抛出
     */
    public AbstractAccessDecision(final GrantedPermissionsProvider grantedPermissionsProvider,
                                  final RequiredPermissionsProvider requiredPermissionsProvider) throws IllegalArgumentException {
        Preconditions.checkArgument(grantedPermissionsProvider != null, "grantedPermissionsProvider == null");
        Preconditions.checkArgument(requiredPermissionsProvider != null, "requiredPermissionsProvider == null");
        this.grantedPermissionsProvider = grantedPermissionsProvider;
        this.requiredPermissionsProvider = requiredPermissionsProvider;
    }

    @Override
    public boolean decide(AccessContext context) {
        final List<Permission> granted = Optional.ofNullable(grantedPermissionsProvider.provide(context))
                .orElse(Collections.emptyList());
        final List<Permission> required = Optional.ofNullable(requiredPermissionsProvider.provide(context))
                .orElse(Collections.emptyList());
        // 基类策略：所有所需权限都必须出现在已授予权限中。
        for (Permission permission : required) {
            if (!granted.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}
