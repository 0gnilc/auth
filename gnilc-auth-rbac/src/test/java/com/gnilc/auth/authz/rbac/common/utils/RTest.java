package com.gnilc.auth.authz.rbac.common.utils;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RTest {

    /**
     * Success responses expose the shared public wrapper contract.
     */
    // TestCaseId: RBAC-COMMON-004
    @Test
    void successUsesSharedWrapperContract() {
        R<String> response = R.success("created");

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo("created");
        assertThat(response.getError()).isNull();
        assertThat(response.getMessage()).isEqualTo("ok");
    }

    /**
     * Failure responses expose the shared public wrapper contract.
     */
    // TestCaseId: RBAC-COMMON-005
    @Test
    void errorUsesSharedWrapperContract() {
        R<?> response = R.error(ResponseCode.ARGUMENT_INVALID, "role code required");

        assertThat(response.getCode()).isEqualTo(ResponseCode.ARGUMENT_INVALID.getBusinessCode());
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isEqualTo("role code required");
        assertThat(response.getMessage()).isEqualTo("role code required");
    }

    /**
     * HTTP Status 数字不应作为响应体业务码。
     */
    // TestCaseId: RBAC-COMMON-006
    @Test
    void rejectHttpStatusLikeBusinessCode() {
        assertThatThrownBy(() -> R.error(400, "bad request"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessCode must be 0 or >= 10000");
    }
}
