package com.gnilc.auth.authz.rbac.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RTest {
    @Test
    void separatesBusinessCodeFromPayload() {
        R<String> success = R.success("created");
        R<Void> error = R.error(10001, "bad input");

        assertThat(success.getCode()).isZero();
        assertThat(success.getData()).isEqualTo("created");
        assertThat(error.getCode()).isEqualTo(10001);
        assertThat(error.getError()).isEqualTo("bad input");
        assertThatThrownBy(() -> R.error(400, "transport status is not a business code"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
