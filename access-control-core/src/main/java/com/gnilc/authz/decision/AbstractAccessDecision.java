package com.gnilc.authz.decision;

import com.google.common.base.Preconditions;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.provider.ResourcePermissionsProvider;
import com.gnilc.authz.provider.SubjectPermissionsProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AbstractAccessDecision implements AccessDecision {
    /**
     * 访问者拥有的权限提供者
     */
    protected final SubjectPermissionsProvider sp;
    /**
     * 访问资源需要的权限提供者
     */
    protected final ResourcePermissionsProvider rp;

    public AbstractAccessDecision(final SubjectPermissionsProvider sp,
                                  final ResourcePermissionsProvider rp) throws IllegalArgumentException {
        Preconditions.checkArgument(sp != null, "sp == null");
        Preconditions.checkArgument(rp != null, "rp == null");
        this.sp = sp;
        this.rp = rp;
    }

    @Override
    public boolean decide() {
        final List<Permission> vps = Optional.ofNullable(sp.provide()).orElse(Collections.emptyList());
        final List<Permission> rps = Optional.ofNullable(rp.provide()).orElse(Collections.emptyList());
        for (Permission targetAccessiblePermission : rps) {
            if (!vps.contains(targetAccessiblePermission)) {
                return false;
            }
        }
        return true;
    }
}
