package com.gnilc.auth.authn.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DefaultAccessPrincipalTest {

    // 字符串主体标识应原样输出为 AccessPrincipal identifier/name。
    // TestCaseId: CORE-AUTHN-001
    @Test
    void createPrincipalFromStringIdentifier() {
        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of("admin:1001");

        assertThat(principal.getIdentifier()).isEqualTo("admin:1001");
        assertThat(principal.getName()).isEqualTo("admin:1001");
        assertThat(principal.getAttributes()).isEmpty();
    }

    // 数字 user_id 应统一转为字符串主体标识，供授权模块按字符串身份事实处理。
    // TestCaseId: CORE-AUTHN-002
    @Test
    void createPrincipalFromLongIdentifier() {
        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of(1001L, Map.of("source", "session"));

        assertThat(principal.getIdentifier()).isEqualTo("1001");
        assertThat(principal.getName()).isEqualTo("1001");
        assertThat(principal.getAttributes()).containsExactly(Map.entry("source", "session"));
    }

    // 主体补充事实应复制并只读，避免认证完成后被调用方篡改。
    // TestCaseId: CORE-AUTHN-003
    @Test
    void defensivelyCopyAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("tenant", "default");

        DefaultAccessPrincipal principal = DefaultAccessPrincipal.of("1001", attributes);
        attributes.put("tenant", "changed");

        assertThat(principal.getAttributes()).containsExactly(Map.entry("tenant", "default"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> principal.getAttributes().put("new", "value"));
    }

    // null 标识不是合法认证主体，必须在创建时失败。
    // TestCaseId: CORE-AUTHN-004
    @Test
    void requireIdentifier() {
        assertThatNullPointerException()
                .isThrownBy(() -> DefaultAccessPrincipal.of((String) null))
                .withMessage("identifier == null");
        assertThatNullPointerException()
                .isThrownBy(() -> DefaultAccessPrincipal.of((Long) null))
                .withMessage("identifier == null");
    }
}
