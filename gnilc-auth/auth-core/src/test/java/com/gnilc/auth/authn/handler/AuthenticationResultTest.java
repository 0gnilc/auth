package com.gnilc.auth.authn.handler;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationResultTest {

    @Test
    void authenticatedResultCarriesPrincipalAndSnapshotOfAttributes() {
        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of("user-1");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("method", "token");

        AuthenticationResult result = AuthenticationResult.authenticated(principal, attributes);
        attributes.put("method", "session");

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(principal);
        assertThat(result.getAttributes()).containsExactly(Map.entry("method", "token"));
        assertThat(result.getReason()).isNull();
        assertThat(result.getCause()).isNull();
        assertThatThrownBy(() -> result.getAttributes().put("extra", true))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failedResultCarriesReasonAndCauseWithoutPrincipal() {
        RuntimeException cause = new RuntimeException("token expired");

        AuthenticationResult result = AuthenticationResult.failed("expired", cause);

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getPrincipal()).isNull();
        assertThat(result.getAttributes()).isEmpty();
        assertThat(result.getReason()).isEqualTo("expired");
        assertThat(result.getCause()).isSameAs(cause);
    }

    @Test
    void authenticatedResultRequiresPrincipal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AuthenticationResult.authenticated(null))
                .withMessage("principal == null!");
    }
}
