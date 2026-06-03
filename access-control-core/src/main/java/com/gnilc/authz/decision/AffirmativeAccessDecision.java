package com.gnilc.authz.decision;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.provider.ResourcePermissionsProvider;
import com.gnilc.authz.provider.SubjectPermissionsProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class AffirmativeAccessDecision extends AbstractAccessDecision {

    public AffirmativeAccessDecision(final SubjectPermissionsProvider sp,
                                     final ResourcePermissionsProvider rp) throws IllegalArgumentException {
        super(sp, rp);
    }

    @Override
    public boolean decide() {
        final List<Permission> sps = Optional.ofNullable(sp.provide()).orElse(Collections.emptyList());
        final List<Permission> rps = Optional.ofNullable(rp.provide()).orElse(Collections.emptyList());
        if (rps.isEmpty()) {
            return true;
        }
        for (Permission rp : rps) {
            if (sps.contains(rp)) {
                return true;
            }
        }
        return false;
    }

}
