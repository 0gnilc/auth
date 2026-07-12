package com.gnilc.auth.authz;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.decision.AbstractAccessDecision;
import com.gnilc.auth.authz.decision.AffirmativeAccessDecision;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.auth.authz.denied.DefaultAccessDenied;
import com.gnilc.auth.authz.provider.DelegatingGrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.DelegatingRequiredPermissionsProvider;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCoreTest {
    private final AccessContext context =
            new AccessContext(new AccessIdentity("9", null), new AccessTarget("/orders", "GET"));

    @Test
    void decisionsImplementAllAndAnyRequiredPermissionPolicies() {
        Permission read = new Permission("order:read");
        Permission write = new Permission("order:write");
        GrantedPermissionsProvider granted = ignored -> List.of(read);
        RequiredPermissionsProvider both = ignored -> List.of(read, write);

        assertThat(new AbstractAccessDecision(granted, both).decide(context)).isFalse();
        assertThat(new AffirmativeAccessDecision(granted, both).decide(context)).isTrue();
        assertThat(new AffirmativeAccessDecision(granted, ignored -> List.of()).decide(context)).isTrue();
        assertThat(new AbstractAccessDecision(ignored -> null, ignored -> null).decide(context)).isTrue();
    }

    @Test
    void delegatingProvidersFilterUnsupportedSourcesAndDeduplicatePermissions() {
        Permission shared = new Permission("shared");
        GrantedPermissionsProvider supported = ignored -> List.of(shared, shared);
        GrantedPermissionsProvider unsupported = new GrantedPermissionsProvider() {
            @Override
            public boolean supports(AccessContext ignored) {
                return false;
            }

            @Override
            public List<Permission> provide(AccessContext ignored) {
                return List.of(new Permission("hidden"));
            }
        };
        RequiredPermissionsProvider required = ignored -> List.of(shared, shared);

        assertThat(new DelegatingGrantedPermissionsProvider(Set.of(supported, unsupported)).provide(context))
                .containsExactly(shared);
        assertThat(new DelegatingRequiredPermissionsProvider(Set.of(required)).provide(context))
                .containsExactly(shared);
    }

    @Test
    void deniedHandlersRunInOrderAndOnlyWhenSupported() {
        List<String> calls = new ArrayList<>();
        AccessDeniedContext deniedContext = new AccessDeniedContext() {
        };
        AccessDeniedHandler late = orderedHandler(20, true, "late", calls);
        AccessDeniedHandler skipped = orderedHandler(10, false, "skipped", calls);
        AccessDeniedHandler early = orderedHandler(0, true, "early", calls);

        new DefaultAccessDenied(List.of(late, skipped, early)).denied(context, deniedContext);

        assertThat(calls).containsExactly("early", "late");
        new DefaultAccessDenied(null).denied(context, deniedContext);
    }

    private AccessDeniedHandler orderedHandler(int order, boolean supports, String name, List<String> calls) {
        class OrderedHandler implements AccessDeniedHandler, Ordered {
            @Override
            public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
                return supports;
            }

            @Override
            public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
                calls.add(name);
            }

            @Override
            public int getOrder() {
                return order;
            }
        }
        return new OrderedHandler();
    }
}
