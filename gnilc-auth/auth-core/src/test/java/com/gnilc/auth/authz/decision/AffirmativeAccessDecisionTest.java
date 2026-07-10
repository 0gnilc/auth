package com.gnilc.auth.authz.decision;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AffirmativeAccessDecisionTest {

    // Affirmative 策略：访问目标没有所需权限时默认放行。
    // TestCaseId: CORE-AUTHZ-033
    @Test
    void allowAccessWhenTargetRequiresNoPermissions() {
        AccessDecision decision = new AffirmativeAccessDecision(
                context -> List.of(new Permission("user:read")),
                context -> List.of()
        );

        boolean allowed = decision.decide(accessContext());

        assertThat(allowed).isTrue();
    }

    // 只要已授予权限命中任一所需权限，即可通过。
    // TestCaseId: CORE-AUTHZ-034
    @Test
    void allowAccessWhenGrantedPermissionsContainAnyRequiredPermission() {
        AccessDecision decision = new AffirmativeAccessDecision(
                context -> List.of(new Permission("user:read"), new Permission("menu:view")),
                context -> List.of(new Permission("role:write"), new Permission("menu:view"))
        );

        boolean allowed = decision.decide(accessContext());

        assertThat(allowed).isTrue();
    }

    // TestCaseId: CORE-AUTHZ-035
    @Test
    void denyAccessWhenGrantedPermissionsDoNotContainRequiredPermissions() {
        AccessDecision decision = new AffirmativeAccessDecision(
                context -> List.of(new Permission("user:read")),
                context -> List.of(new Permission("role:write"))
        );

        boolean allowed = decision.decide(accessContext());

        assertThat(allowed).isFalse();
    }

    // 决策器必须把同一个访问上下文传给两侧 provider。
    // TestCaseId: CORE-AUTHZ-036
    @Test
    void passAccessContextToPermissionProviders() {
        AccessContext context = accessContext();
        AtomicReference<AccessContext> grantedContext = new AtomicReference<>();
        AtomicReference<AccessContext> requiredContext = new AtomicReference<>();
        AccessDecision decision = new AffirmativeAccessDecision(
                candidate -> {
                    grantedContext.set(candidate);
                    return List.of(new Permission("user:read"));
                },
                candidate -> {
                    requiredContext.set(candidate);
                    return List.of(new Permission("user:read"));
                }
        );

        decision.decide(context);

        assertThat(grantedContext).hasValue(context);
        assertThat(requiredContext).hasValue(context);
    }

    // provider 返回 null 时按空权限集合处理，目标没有所需权限仍默认放行。
    // TestCaseId: CORE-AUTHZ-037
    @Test
    void treatNullRequiredPermissionsAsEmptyAndAllow() {
        AccessDecision decision = new AffirmativeAccessDecision(
                context -> List.of(new Permission("user:read")),
                context -> null
        );

        boolean allowed = decision.decide(accessContext());

        assertThat(allowed).isTrue();
    }

    // 已授予权限 provider 返回 null 时按空集合处理；目标有权限要求则拒绝。
    // TestCaseId: CORE-AUTHZ-038
    @Test
    void treatNullGrantedPermissionsAsEmptyAndDenyRequiredTarget() {
        AccessDecision decision = new AffirmativeAccessDecision(
                context -> null,
                context -> List.of(new Permission("user:read"))
        );

        boolean allowed = decision.decide(accessContext());

        assertThat(allowed).isFalse();
    }

    private AccessContext accessContext() {
        return new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of())
        );
    }
}
