package com.gnilc.system.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSessionTokenCodecTest {
    private final AdminSessionTokenCodec tokenCodec = new AdminSessionTokenCodec();

    // TestCaseId: SYS-TOKEN-001
    @Test
    void createsUserIdPrefixedUrlSafeTokenWithThirtyTwoRandomBytes() {
        String token = tokenCodec.issue(1001L);

        assertThat(token).startsWith("sys_admin.1001.");
        String randomPart = token.substring("sys_admin.1001.".length());
        assertThat(randomPart).hasSize(43);
        assertThat(randomPart).doesNotContain("+").doesNotContain("/").doesNotContain("=");
    }

    // TestCaseId: SYS-TOKEN-002
    @Test
    void createsDifferentTokensForConcurrentLogins() {
        String first = tokenCodec.issue(1001L);
        String second = tokenCodec.issue(1001L);

        assertThat(first).isNotEqualTo(second);
    }

    // TestCaseId: SYS-TOKEN-003
    @Test
    void resolvesUserIdFromToken() {
        Long userId = tokenCodec.resolve("sys_admin.1001.abcdefghijklmnopqrstuvwxyzABCDEFGHI");

        assertThat(userId).isEqualTo(1001L);
    }

    // TestCaseId: SYS-TOKEN-004
    @Test
    void matchesOnlyAdminNamespacedTokens() {
        assertThat(tokenCodec.matches("sys_admin.1001.random")).isTrue();
        assertThat(tokenCodec.matches("1001.random")).isFalse();
        assertThat(tokenCodec.matches(null)).isFalse();
    }

    // TestCaseId: SYS-TOKEN-005
    @Test
    void rejectsMalformedTokens() {
        assertThatThrownBy(() -> tokenCodec.resolve("sys_admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid admin token");
        assertThatThrownBy(() -> tokenCodec.resolve("1001.random"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid admin token");
    }
}
