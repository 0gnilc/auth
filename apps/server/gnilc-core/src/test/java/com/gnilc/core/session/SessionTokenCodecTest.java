package com.gnilc.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证令牌随机性、身份域前缀及用户 ID 解析边界；可解析不等同于通过 Redis 会话认证。 */
class SessionTokenCodecTest {
    private final SessionTokenCodec codec = new SessionTokenCodec("sys_admin");

    @Test
    void issuedTokensAreNamespacedUniqueAndResolveTheirUser() {
        String first = codec.issue(42L);
        String second = codec.issue(42L);

        assertThat(first).startsWith("sys_admin.42.");
        assertThat(second).isNotEqualTo(first);
        assertThat(codec.matches(first)).isTrue();
        assertThat(codec.resolve(first)).isEqualTo(42L);
    }

    @Test
    void foreignAndMalformedTokensAreRejected() {
        assertThat(codec.matches(null)).isFalse();
        assertThat(codec.matches("foreign.42.value")).isFalse();
        assertThatThrownBy(() -> codec.resolve("sys_admin.not-a-number.value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.issue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
