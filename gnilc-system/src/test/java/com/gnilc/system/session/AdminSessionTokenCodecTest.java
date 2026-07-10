package com.gnilc.system.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSessionTokenCodecTest {
    private final AdminSessionTokenCodec tokenCodec = new AdminSessionTokenCodec();

    @Test
    void issuesDistinctUrlSafeTokensContainingTheAdminUserId() {
        String first = tokenCodec.issue(1001L);
        String second = tokenCodec.issue(1001L);

        assertThat(first).startsWith("sys_admin.1001.").isNotEqualTo(second);
        assertThat(first.substring("sys_admin.1001.".length()))
                .hasSize(43)
                .doesNotContain("+", "/", "=");
        assertThat(tokenCodec.resolve(first)).isEqualTo(1001L);
    }

    @Test
    void recognizesOnlyTheAdminTokenNamespace() {
        assertThat(tokenCodec.matches("sys_admin.1001.random")).isTrue();
        assertThat(tokenCodec.matches("1001.random")).isFalse();
        assertThat(tokenCodec.matches(null)).isFalse();
    }

    @Test
    void rejectsMissingUserIdsMalformedUserIdsAndMissingRandomValues() {
        assertThatThrownBy(() -> tokenCodec.resolve("sys_admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid admin token");
        assertThatThrownBy(() -> tokenCodec.resolve("sys_admin.user.random"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid admin token");
        assertThatThrownBy(() -> tokenCodec.resolve("sys_admin.1001."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid admin token");
    }

    @Test
    void rejectsNullUserIdsWhenIssuingTokens() {
        assertThatThrownBy(() -> tokenCodec.issue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId == null");
    }
}
