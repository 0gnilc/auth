package com.gnilc.auth.authn.handler;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证认证成功结果保存不可变属性快照，失败结果仅保留原因和异常而不携带主体。 */
class AuthenticationResultTest {
    @Test
    void authenticatedResultExposesAnImmutableAttributeSnapshot() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenant", "north");
        AccessPrincipal principal = DefaultAccessPrincipal.of(42L);

        AuthenticationResult result = AuthenticationResult.authenticated(principal, attributes);
        attributes.put("tenant", "south");

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(principal);
        assertThat(result.getAttributes()).containsEntry("tenant", "north");
        assertThatThrownBy(() -> result.getAttributes().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failedResultCarriesReasonAndCauseWithoutPrincipal() {
        IllegalStateException cause = new IllegalStateException("offline");

        AuthenticationResult result = AuthenticationResult.failed("invalid token", cause);

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getPrincipal()).isNull();
        assertThat(result.getReason()).isEqualTo("invalid token");
        assertThat(result.getCause()).isSameAs(cause);
        assertThat(result.getAttributes()).isEmpty();
    }
}
