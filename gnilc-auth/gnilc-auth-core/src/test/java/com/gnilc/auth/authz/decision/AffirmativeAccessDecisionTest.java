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

class AffirmativeAccessDecisionTest {
    private final AccessContext context =
            new AccessContext(new AccessIdentity("9", null), new AccessTarget("/orders", "GET"));

    @Test
    void allowsAnyMatchingPermissionAndEmptyRequirements() {
        Permission read = new Permission("order:read");
        Permission write = new Permission("order:write");
        GrantedPermissionsProvider granted = ignored -> List.of(read);
        RequiredPermissionsProvider required = ignored -> List.of(read, write);

        assertThat(new AffirmativeAccessDecision(granted, required).decide(context)).isTrue();
        assertThat(new AffirmativeAccessDecision(granted, ignored -> List.of()).decide(context)).isTrue();
    }
}
