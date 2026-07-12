package com.gnilc.auth.authz.decision;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAccessDecisionTest {
    private final AccessContext context =
            new AccessContext(new AccessIdentity("9", null), new AccessTarget("/orders", "GET"));

    @Test
    void requiresAllPermissionsAndAllowsEmptyRequirements() {
        Permission read = new Permission("order:read");
        Permission write = new Permission("order:write");
        GrantedPermissionsProvider granted = ignored -> List.of(read);
        RequiredPermissionsProvider required = ignored -> List.of(read, write);

        assertThat(new AbstractAccessDecision(granted, required).decide(context)).isFalse();
        assertThat(new AbstractAccessDecision(ignored -> null, ignored -> null).decide(context)).isTrue();
    }
}
