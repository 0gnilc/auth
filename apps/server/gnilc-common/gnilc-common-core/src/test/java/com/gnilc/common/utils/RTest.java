package com.gnilc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RTest {
    @Test
    void storesCodeAndPayloadWithoutRestrictingCodeRanges() {
        R<String> success = R.success("created");
        R<Void> error = R.error(10001, "bad input");
        R<Void> transport = R.error(400, "transport status is also allowed");
        R<Void> unspecified = new R<>(null, "unspecified", null);

        assertThat(success.getCode()).isZero();
        assertThat(success.getData()).isEqualTo("created");
        assertThat(error.getCode()).isEqualTo(10001);
        assertThat(error.getError()).isEqualTo("bad input");
        assertThat(transport.getCode()).isEqualTo(400);
        assertThat(unspecified.getCode()).isNull();
    }
}
