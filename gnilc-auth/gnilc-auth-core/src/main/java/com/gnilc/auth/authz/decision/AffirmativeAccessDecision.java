package com.gnilc.auth.authz.decision;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 肯定式访问决策器。
 * <p>
 * 访问目标没有所需权限时放行；否则只要命中任一所需权限即可放行。
 */
public class AffirmativeAccessDecision extends AbstractAccessDecision {

    /**
     * 创建肯定式访问决策器。
     *
     * @param grantedPermissionsProvider  已授予权限提供者
     * @param requiredPermissionsProvider 所需权限提供者
     * @throws IllegalArgumentException provider 为空时抛出
     */
    public AffirmativeAccessDecision(final GrantedPermissionsProvider grantedPermissionsProvider,
                                     final RequiredPermissionsProvider requiredPermissionsProvider) throws IllegalArgumentException {
        super(grantedPermissionsProvider, requiredPermissionsProvider);
    }

    @Override
    public boolean decide(AccessContext context) {
        final List<Permission> granted = Optional.ofNullable(grantedPermissionsProvider.provide(context))
                .orElse(Collections.emptyList());
        final List<Permission> required = Optional.ofNullable(requiredPermissionsProvider.provide(context))
                .orElse(Collections.emptyList());
        if (required.isEmpty()) {
            return true;
        }
        // Affirmative 策略：任一所需权限被授予即可通过。
        for (Permission permission : required) {
            if (granted.contains(permission)) {
                return true;
            }
        }
        return false;
    }

}
