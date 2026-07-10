package com.gnilc.auth.authz.decision;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.provider.GrantedPermissionsProvider;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class AccessDecisionTest {
    private static final AccessContext CONTEXT = mock(AccessContext.class);
    private static final Permission READ = new Permission("read");
    private static final Permission WRITE = new Permission("write");

    @Test
    void abstractDecisionRequiresEveryPermission() {
        AbstractAccessDecision decision = new AbstractAccessDecision(
                granted(READ),
                required(READ, WRITE)
        );

        assertThat(decision.decide(CONTEXT)).isFalse();
    }

    @Test
    void abstractDecisionAllowsWhenRequirementsAreEmptyOrFullyGranted() {
        AbstractAccessDecision fullyGranted = new AbstractAccessDecision(
                granted(READ, WRITE),
                required(READ, WRITE)
        );
        AbstractAccessDecision noRequirements = new AbstractAccessDecision(
                context -> null,
                context -> null
        );

        assertThat(fullyGranted.decide(CONTEXT)).isTrue();
        assertThat(noRequirements.decide(CONTEXT)).isTrue();
    }

    @Test
    void affirmativeDecisionAllowsAnyMatchingPermission() {
        AffirmativeAccessDecision decision = new AffirmativeAccessDecision(
                granted(WRITE),
                required(READ, WRITE)
        );

        assertThat(decision.decide(CONTEXT)).isTrue();
    }

    @Test
    void affirmativeDecisionDeniesWhenNoPermissionMatches() {
        AffirmativeAccessDecision decision = new AffirmativeAccessDecision(
                granted(READ),
                required(WRITE)
        );

        assertThat(decision.decide(CONTEXT)).isFalse();
    }

    @Test
    void affirmativeDecisionAllowsWhenRequirementsAreAbsent() {
        AffirmativeAccessDecision decision = new AffirmativeAccessDecision(
                context -> null,
                context -> null
        );

        assertThat(decision.decide(CONTEXT)).isTrue();
    }

    @Test
    void nullGrantedPermissionsAreEmptyAndCannotSatisfyRequirements() {
        AbstractAccessDecision allRequired = new AbstractAccessDecision(
                context -> null,
                required(READ)
        );
        AffirmativeAccessDecision anyRequired = new AffirmativeAccessDecision(
                context -> null,
                required(READ)
        );

        assertThat(allRequired.decide(CONTEXT)).isFalse();
        assertThat(anyRequired.decide(CONTEXT)).isFalse();
    }

    @Test
    void nullRequiredPermissionsAreEmptyAndAllowAccess() {
        AbstractAccessDecision allRequired = new AbstractAccessDecision(
                granted(),
                context -> null
        );
        AffirmativeAccessDecision anyRequired = new AffirmativeAccessDecision(
                granted(),
                context -> null
        );

        assertThat(allRequired.decide(CONTEXT)).isTrue();
        assertThat(anyRequired.decide(CONTEXT)).isTrue();
    }

    @Test
    void constructorRejectsMissingProviders() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AbstractAccessDecision(null, required()))
                .withMessage("grantedPermissionsProvider == null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AbstractAccessDecision(granted(), null))
                .withMessage("requiredPermissionsProvider == null");
    }

    private static GrantedPermissionsProvider granted(Permission... permissions) {
        return context -> List.of(permissions);
    }

    private static RequiredPermissionsProvider required(Permission... permissions) {
        return context -> List.of(permissions);
    }
}
