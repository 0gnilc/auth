package com.gnilc.auth.authn.handler;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AuthenticationResultTest {

    // 认证成功结果应保留主体、复制补充事实，并禁止外部修改结果属性。
    // TestCaseId: CORE-AUTHN-005
    @Test
    void authenticatedResultKeepsPrincipalAndDefensivelyCopiesAttributes() {
        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of("1001");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("tenant", "admin");

        AuthenticationResult result = AuthenticationResult.authenticated(principal, attributes);
        attributes.put("tenant", "changed");

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(principal);
        assertThat(result.getAttributes()).containsExactly(Map.entry("tenant", "admin"));
        assertThat(result.getReason()).isNull();
        assertThat(result.getCause()).isNull();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> result.getAttributes().put("new", "value"));
    }

    // 成功认证必须携带主体，避免后续链路拿到“成功但无身份”的矛盾结果。
    // TestCaseId: CORE-AUTHN-006
    @Test
    void requirePrincipalForAuthenticatedResult() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AuthenticationResult.authenticated(null))
                .withMessage("principal == null!");
    }

    // 认证失败结果只表达失败事实，不应携带主体或可变属性。
    // TestCaseId: CORE-AUTHN-007
    @Test
    void failedResultKeepsReasonCauseAndEmptyAttributes() {
        IllegalStateException cause = new IllegalStateException("token expired");

        AuthenticationResult result = AuthenticationResult.failed("bad token", cause);

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getPrincipal()).isNull();
        assertThat(result.getReason()).isEqualTo("bad token");
        assertThat(result.getCause()).isSameAs(cause);
        assertThat(result.getAttributes()).isEmpty();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> result.getAttributes().put("new", "value"));
    }
}
